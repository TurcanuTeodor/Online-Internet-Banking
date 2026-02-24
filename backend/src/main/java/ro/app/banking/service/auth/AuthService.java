package ro.app.banking.service.auth;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.Claims;
import ro.app.banking.dto.auth.LoginRequest;
import ro.app.banking.dto.auth.LoginResponse;
import ro.app.banking.dto.auth.RegisterRequest;
import ro.app.banking.dto.auth.RefreshTokenResponse;
import ro.app.banking.dto.auth.TwoFaSetupResponse;
import ro.app.banking.exception.AuthenticationException;
import ro.app.banking.exception.ResourceNotFoundException;
import ro.app.banking.model.entity.Client;
import ro.app.banking.model.entity.RefreshToken;
import ro.app.banking.model.entity.User;
import ro.app.banking.model.enums.ClientType;
import ro.app.banking.model.enums.Role;
import ro.app.banking.model.enums.SexType;
import ro.app.banking.repository.ClientRepository;
import ro.app.banking.repository.UserRepository;
import ro.app.banking.security.jwt.JwtService;
import ro.app.banking.security.jwt.RefreshTokenService;

@Service
public class AuthService {
    private final UserRepository userRepo;
    private final ClientRepository clientRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TotpService totpService;

    public AuthService(
            UserRepository userRepo,
            ClientRepository clientRepo,
            PasswordEncoder encoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            TotpService totpService
    ) {
        this.userRepo = userRepo;
        this.clientRepo = clientRepo;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.totpService = totpService;
    }

    @Transactional
    public void register(RegisterRequest req){
        if(userRepo.existsByUsernameOrEmail(req.getUsernameOrEmail())){
            throw new IllegalArgumentException("Username/Email already used");
        }

        Client client;
        
        // If clientId provided, link to existing client
        if (req.getClientId() != null) {
            client = clientRepo.findById(req.getClientId())
                                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        } else {
            // Create new client with provided details
            if (req.getFirstName() == null || req.getFirstName().isBlank()) {
                throw new IllegalArgumentException("First name is required");
            }
            if (req.getLastName() == null || req.getLastName().isBlank()) {
                throw new IllegalArgumentException("Last name is required");
            }
            if (req.getSexCode() == null || req.getSexCode().isBlank()) {
                throw new IllegalArgumentException("Sex code is required");
            }
            if (req.getClientTypeCode() == null || req.getClientTypeCode().isBlank()) {
                throw new IllegalArgumentException("Client type code is required");
            }
            
            client = new Client();
            client.setFirstName(req.getFirstName());
            client.setLastName(req.getLastName());
            client.setSexType(SexType.fromCode(req.getSexCode()));
            client.setClientType(ClientType.fromCode(req.getClientTypeCode()));
            client.setActive(true);
            client = clientRepo.save(client);
        }
        
        try {
            User u = new User();
            u.setClient(client);
            u.setUsernameOrEmail(req.getUsernameOrEmail());
            u.setPasswordHash(encoder.encode(req.getPassword()));
            u.setRole(Role.USER);
            userRepo.save(u);
        } catch (DataIntegrityViolationException ex) {
            //covers race between exists() and save(); DB unique constraint should enforce this
            throw new IllegalArgumentException("Username/Email already used");
        }
    }

    public LoginResponse login(LoginRequest req){
        User user = userRepo.findByUsernameOrEmail(req.getUsernameOrEmail())
                            .orElseThrow(()-> new AuthenticationException("Invalid credentials"));
        
        if(!encoder.matches(req.getPassword(), user.getPasswordHash())){
            throw new AuthenticationException("Invalid credentials");
        }

        if (user.getClient() == null) {
            throw new IllegalStateException("User has no associated client");
        }

        Long clientId= user.getClient().getId();

        //no active 2FA -> issue final JWT
        if(!user.isTwoFactorEnabled()){
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
            
            String token = jwtService.generateToken(
                user.getUsernameOrEmail(),
                Map.of(
                    "role", user.getRole().name(),
                    "clientId", clientId,
                    "2fa", "ok",
                    "2fa_verified", false
                )
            );

            return new LoginResponse(false, token, refreshToken.getToken(), clientId, user.getRole().name());
        }

        //active 2FA -> short temp token, purpose-bound
        String tempToken = jwtService.generateTempToken(
            user.getUsernameOrEmail(),
            Map.of(
                "clientId", clientId,
                "2fa", "pending",
                "purpose", "2fa"
            )
        );

        return new LoginResponse(true, tempToken, null, clientId, user.getRole().name());
    }

    //---------------2Fa Setup / Confirm / Verify-------------------------------

    @Transactional
    public TwoFaSetupResponse setup2fa(String usernameOrEmail){
        User user = userRepo.findByUsernameOrEmail(usernameOrEmail)
                            .orElseThrow(()-> new ResourceNotFoundException("User not found"));
            
        String secret = totpService.generateSecret();
        String otpauth = totpService.buildOtpAuthUrl(user.getUsernameOrEmail(), secret);

        user.setTwoFactorSecret(secret);
        user.setTwoFactorEnabled(false); //not confirmed yet
        userRepo.save(user);

        return new TwoFaSetupResponse(otpauth, secret);
    }

    @Transactional
    public void confirm2fa(String usernameOrEmail, String code){
        User user = userRepo.findByUsernameOrEmail(usernameOrEmail)
                            .orElseThrow(()-> new ResourceNotFoundException("User not found"));

        if (user.getTwoFactorSecret() == null) {
            throw new IllegalStateException("2FA not initialized for user");
        }

        boolean ok = totpService.verifyCode(user.getTwoFactorSecret(), code);
        if (!ok) {
            throw new AuthenticationException("Invalid 2FA code");
        }

        user.setTwoFactorEnabled(true);
        userRepo.save(user);
    }

    public LoginResponse verify2fa(String tempToken, String code) {
        if (!jwtService.isValid(tempToken)) {
            throw new AuthenticationException("Invalid or expired token");
        }

        Claims claims = jwtService.parseClaims(tempToken);
        Object status = claims.get("2fa");
        Object purpose = claims.get("purpose");
        if (!"pending".equals(status) || (purpose != null && !"2fa".equals(purpose))) {
            throw new IllegalArgumentException("Token not valid for 2FA verification");
        }

        String subject = claims.getSubject();
        User user = userRepo.findByUsernameOrEmail(subject)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getTwoFactorSecret() == null) {
            throw new IllegalStateException("2FA not initialized for user");
        }

        boolean ok = totpService.verifyCode(user.getTwoFactorSecret(), code);
        if (!ok) {
            throw new AuthenticationException("Invalid 2FA code");
        }

        Long clientId = user.getClient() != null ? user.getClient().getId() : null;

        String finalToken = jwtService.generateToken(
            user.getUsernameOrEmail(),
            Map.of(
                "role", user.getRole().name(),
                "clientId", clientId,
                "2fa", "ok",
                "2fa_verified", true
            )
        );

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        return new LoginResponse(false, finalToken, refreshToken.getToken(), clientId, user.getRole().name());
    }

    public RefreshTokenResponse refreshToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(refreshTokenValue);
        User user = refreshToken.getUser();
        
        Long clientId = user.getClient() != null ? user.getClient().getId() : null;
        
        boolean twoFaVerified = user.isTwoFactorEnabled();

        String newToken = jwtService.generateToken(
            user.getUsernameOrEmail(),
            Map.of(
                "role", user.getRole().name(),
                "clientId", clientId,
                "2fa", "ok",
                "2fa_verified", twoFaVerified
            )
        );
        
        // Optional: Rotate refresh token
        refreshTokenService.revokeRefreshToken(refreshTokenValue);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);
        
        return new RefreshTokenResponse(newToken, newRefreshToken.getToken());
    }

    public void logout(String refreshTokenValue) {
        try {
            refreshTokenService.revokeRefreshToken(refreshTokenValue);
        } catch (Exception e) {
            // Log and continue - logout should succeed even if token is invalid
        }
    }

}
