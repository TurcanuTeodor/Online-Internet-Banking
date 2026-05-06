package ro.app.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.Claims;
import ro.app.auth.dto.auth.LoginResponse;
import ro.app.auth.dto.twofa.TwoFaSetupResponse;
import ro.app.auth.exception.AuthenticationException;
import ro.app.auth.model.entity.User;
import ro.app.auth.repository.UserRepository;
import ro.app.auth.security.jwt.JwtService;
import ro.app.auth.exception.PreconditionRequiredException;

@Service
public class TwoFaService {

    private final UserRepository userRepo;
    private final JwtService jwtService;
    private final TotpService totpService;
    private final RefreshTokenService refreshTokenService;
    private final AuthService authService;
    private final ServiceEncryptionService serviceEncryption;

    public TwoFaService(
            UserRepository userRepo,
            JwtService jwtService,
            TotpService totpService,
            RefreshTokenService refreshTokenService,
            AuthService authService,
            ServiceEncryptionService serviceEncryption) {
        this.userRepo = userRepo;
        this.jwtService = jwtService;
        this.totpService = totpService;
        this.refreshTokenService = refreshTokenService;
        this.authService = authService;
        this.serviceEncryption = serviceEncryption;
    }

    @Transactional
    public TwoFaSetupResponse setup2fa(String usernameOrEmail) {
        User user = userRepo.findByUsernameOrEmail(usernameOrEmail)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        String secret = totpService.generateSecret();
        String otpauth = totpService.buildOtpAuthUrl(user.getUsernameOrEmail(), secret);

        // Encrypt before persisting — plaintext secret never touches the DB
        user.setTwoFactorSecret(serviceEncryption.encrypt(secret));
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

        // decryptOrLegacy handles legacy plaintext rows that pre-date encryption rollout
        String plainSecret = serviceEncryption.decryptOrLegacy(user.getTwoFactorSecret());
        boolean ok = totpService.verifyCode(plainSecret, code);
        if (!ok) {
            throw new AuthenticationException("Invalid 2FA code");
        }

        // Re-encrypt if secret was still stored as legacy plaintext
        if (plainSecret.equals(user.getTwoFactorSecret())) {
            user.setTwoFactorSecret(serviceEncryption.encrypt(plainSecret));
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

        String plainSecret = serviceEncryption.decryptOrLegacy(user.getTwoFactorSecret());
        boolean ok = totpService.verifyCode(plainSecret, code);
        if (!ok) {
            throw new AuthenticationException("Invalid 2FA code");
        }

        // Re-encrypt legacy plaintext secret opportunistically
        if (plainSecret.equals(user.getTwoFactorSecret())) {
            user.setTwoFactorSecret(serviceEncryption.encrypt(plainSecret));
            userRepo.save(user);
        }

        Long clientId = user.getClientId();

        Object ek = claims.get("ek");

        java.util.Map<String, Object> tokenClaims = new java.util.HashMap<>();
        tokenClaims.put("role", user.getRole().name());
        tokenClaims.put("clientId", clientId);
        tokenClaims.put("2fa", "ok");
        tokenClaims.put("2fa_verified", true);
        if (ek != null) {
            tokenClaims.put("ek", ek);
        }

        String finalToken = jwtService.generateToken(
                user.getUsernameOrEmail(),
                tokenClaims);

        if (ek != null) {
            authService.runPostLoginClientEncryption(clientId, ek.toString());
        }

        String refreshTokenValue = refreshTokenService.createRefreshToken(user);
        return new LoginResponse(false, finalToken, refreshTokenValue, clientId, user.getRole().name());
    }

    public void verifyStepUp(Long clientId, String code) {
        User user = userRepo.findByClientId(clientId)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        if (!user.isTwoFactorEnabled() || user.getTwoFactorSecret() == null) {
            throw new PreconditionRequiredException("2FA must be enabled to perform this action");
        }

        String plainSecret = serviceEncryption.decryptOrLegacy(user.getTwoFactorSecret());
        boolean ok = totpService.verifyCode(plainSecret, code);
        if (!ok) {
            throw new AuthenticationException("Invalid 2FA code");
        }
    }
}
