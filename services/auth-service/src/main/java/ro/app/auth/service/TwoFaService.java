package ro.app.auth.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.Claims;
import ro.app.auth.dto.auth.LoginResponse;
import ro.app.auth.dto.twofa.TwoFaSetupResponse;
import ro.app.auth.exception.AuthenticationException;
import ro.app.auth.model.entity.User;
import ro.app.auth.repository.UserRepository;
import ro.app.auth.security.jwt.JwtService;

@Service
public class TwoFaService {

    private final UserRepository userRepo;
    private final JwtService jwtService;
    private final TotpService totpService;
    private final RefreshTokenService refreshTokenService;

    public TwoFaService(
            UserRepository userRepo,
            JwtService jwtService,
            TotpService totpService,
            RefreshTokenService refreshTokenService) {
        this.userRepo = userRepo;
        this.jwtService = jwtService;
        this.totpService = totpService;
        this.refreshTokenService = refreshTokenService;
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

        Long clientId = user.getClientId();

        String finalToken = jwtService.generateToken(
                user.getUsernameOrEmail(),
                Map.of(
                        "role", user.getRole().name(),
                        "clientId", clientId,
                        "2fa", "ok",
                        "2fa_verified", true
                ));

        String refreshTokenValue = refreshTokenService.createRefreshToken(user);
        return new LoginResponse(false, finalToken, refreshTokenValue, clientId, user.getRole().name());
    }
}
