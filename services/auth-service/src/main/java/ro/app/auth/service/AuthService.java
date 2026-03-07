package ro.app.auth.service;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.Claims;
import ro.app.auth.dto.LoginRequest;
import ro.app.auth.dto.LoginResponse;
import ro.app.auth.dto.RegisterRequest;
import ro.app.auth.dto.RefreshTokenResponse;
import ro.app.auth.dto.TwoFaSetupResponse;
import ro.app.auth.exception.AuthenticationException;
import ro.app.auth.model.entity.RefreshToken;
import ro.app.auth.model.entity.User;
import ro.app.auth.model.enums.Role;
import ro.app.auth.repository.UserRepository;
import ro.app.auth.security.jwt.JwtService;
import ro.app.auth.security.jwt.RefreshTokenService;

@Service
public class AuthService {
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TotpService totpService;

    public AuthService(
            UserRepository userRepo,
            PasswordEncoder encoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            TotpService totpService
    ) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.totpService = totpService;
    }

    @Transactional
    public void register(RegisterRequest req) {
        if (userRepo.existsByUsernameOrEmail(req.getUsernameOrEmail())) {
            throw new IllegalArgumentException("Username/Email already used");
        }

        // In distributed arch: clientId is REQUIRED
        // Client creation is handled by user-service
        if (req.getClientId() == null) {
            throw new IllegalArgumentException("clientId is required. Create client via user-service first.");
        }

        try {
            User u = new User();
            u.setClientId(req.getClientId());
            u.setUsernameOrEmail(req.getUsernameOrEmail());
            u.setPasswordHash(encoder.encode(req.getPassword()));
            u.setRole(Role.USER);
            userRepo.save(u);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Username/Email already used");
        }
    }

    public LoginResponse login(LoginRequest req) {
        User user = userRepo.findByUsernameOrEmail(req.getUsernameOrEmail())
                            .orElseThrow(() -> new AuthenticationException("Invalid credentials"));

        if (!encoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid credentials");
        }

        Long clientId = user.getClientId();  

        if (!user.isTwoFactorEnabled()) {
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

    @Transactional
    public TwoFaSetupResponse setup2fa(String usernameOrEmail) {
        User user = userRepo.findByUsernameOrEmail(usernameOrEmail)
                            .orElseThrow(() -> new AuthenticationException("User not found"));

        String secret = totpService.generateSecret();
        String otpauth = totpService.buildOtpAuthUrl(user.getUsernameOrEmail(), secret);

        user.setTwoFactorSecret(secret);
        user.setTwoFactorEnabled(false);
        userRepo.save(user);

        return new TwoFaSetupResponse(otpauth, secret);
    }

    @Transactional
    public void confirm2fa(String usernameOrEmail, String code) {
        User user = userRepo.findByUsernameOrEmail(usernameOrEmail)
                            .orElseThrow(() -> new AuthenticationException("User not found"));

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
                            .orElseThrow(() -> new AuthenticationException("User not found"));

        if (user.getTwoFactorSecret() == null) {
            throw new IllegalStateException("2FA not initialized for user");
        }

        boolean ok = totpService.verifyCode(user.getTwoFactorSecret(), code);
        if (!ok) {
            throw new AuthenticationException("Invalid 2FA code");
        }

        Long clientId = user.getClientId();  // ← CHANGED

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

        Long clientId = user.getClientId();  // ← CHANGED
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

        refreshTokenService.revokeRefreshToken(refreshTokenValue);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        return new RefreshTokenResponse(newToken, newRefreshToken.getToken());
    }

    public void logout(String refreshTokenValue) {
        try {
            refreshTokenService.revokeRefreshToken(refreshTokenValue);
        } catch (Exception e) {
            // Log and continue
        }
    }
}