package ro.app.auth.service;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.app.auth.dto.auth.LoginRequest;
import ro.app.auth.dto.auth.LoginResponse;
import ro.app.auth.dto.auth.RegisterRequest;
import ro.app.auth.exception.AuthenticationException;
import ro.app.auth.model.entity.RefreshToken;
import ro.app.auth.model.entity.User;
import ro.app.auth.model.enums.Role;
import ro.app.auth.repository.UserRepository;
import ro.app.auth.security.jwt.JwtService;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepo,
            PasswordEncoder encoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public void register(RegisterRequest req) {
        if (userRepo.existsByUsernameOrEmail(req.getUsernameOrEmail())) {
            throw new IllegalArgumentException("Username/Email already used");
        }

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
                    ));

            return new LoginResponse(false, token, refreshToken.getToken(), clientId, user.getRole().name());
        }

        String tempToken = jwtService.generateTempToken(
                user.getUsernameOrEmail(),
                Map.of(
                        "clientId", clientId,
                        "2fa", "pending",
                        "purpose", "2fa"
                ));

        return new LoginResponse(true, tempToken, null, clientId, user.getRole().name());
    }

    public void logout(String refreshTokenValue) {
        try {
            refreshTokenService.revokeRefreshToken(refreshTokenValue);
        } catch (Exception e) {
            // Log and continue
        }
    }
}
