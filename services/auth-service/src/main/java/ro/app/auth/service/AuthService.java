package ro.app.auth.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import ro.app.auth.dto.auth.ChangePasswordRequest;
import ro.app.auth.dto.auth.LoginRequest;
import ro.app.auth.dto.auth.LoginResponse;
import ro.app.auth.dto.auth.RegisterRequest;
import ro.app.auth.exception.AuthenticationException;
import ro.app.auth.model.entity.User;
import ro.app.auth.model.enums.Role;
import ro.app.auth.repository.UserRepository;
import ro.app.auth.security.jwt.JwtService;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.services.client.url:http://localhost:8082}")
    private String clientServiceUrl;

    @Value("${app.internal.api-secret:change-me-internal-secret}")
    private String internalApiSecret;

    /** Must match client-service {@code encryption.key} for sign-up + legacy DB rows. */
    @Value("${encryption.legacy-key:}")
    private String legacyEncryptionKey;

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

    // ======================== REGISTRATION ========================

    @Transactional
    public void register(RegisterRequest req) {
        validatePassword(req.getPassword());

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
            u.setEncryptionSalt(generateSalt());
            userRepo.save(u);

            String ek = deriveEncryptionKey(req.getPassword(), u.getEncryptionSalt());
            if (legacyEncryptionKey != null && !legacyEncryptionKey.isBlank()) {
                reEncryptClientData(u.getClientId(), legacyEncryptionKey, ek);
            }
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Username/Email already used");
        }
    }

    // ======================== LOGIN ========================

    public LoginResponse login(LoginRequest req) {
        User user = userRepo.findByUsernameOrEmail(req.getUsernameOrEmail())
                .orElseThrow(() -> new AuthenticationException("Invalid credentials"));

        if (!user.isEnabled()) {
            throw new AuthenticationException("Account disabled");
        }

        if (!encoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid credentials");
        }

        // Derive the per-user encryption key from password + salt
        String encryptionKey = deriveEncryptionKey(req.getPassword(), user.getEncryptionSalt());
        Long clientId = user.getClientId();

        if (!user.isTwoFactorEnabled()) {
            String refreshTokenValue = refreshTokenService.createRefreshToken(user);

            String token = jwtService.generateToken(
                    user.getUsernameOrEmail(),
                    Map.of(
                            "role", user.getRole().name(),
                            "clientId", clientId,
                            "2fa", "ok",
                            "2fa_verified", false,
                            "ek", encryptionKey
                    ));

            runPostLoginClientEncryption(clientId, encryptionKey);

            return new LoginResponse(false, token, refreshTokenValue, clientId, user.getRole().name());
        }

        // For 2FA flow: include encryption key in temp token so it survives verification
        String tempToken = jwtService.generateTempToken(
                user.getUsernameOrEmail(),
                Map.of(
                        "clientId", clientId,
                        "2fa", "pending",
                        "purpose", "2fa",
                        "ek", encryptionKey
                ));

        return new LoginResponse(true, tempToken, null, clientId, user.getRole().name());
    }

    // ======================== CHANGE PASSWORD ========================

    @Transactional
    public void changePassword(String username, ChangePasswordRequest req) {
        User user = userRepo.findByUsernameOrEmail(username)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        // Verify old password
        if (!encoder.matches(req.getOldPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect");
        }

        validatePassword(req.getNewPassword());

        // Derive old and new encryption keys
        String oldKey = deriveEncryptionKey(req.getOldPassword(), user.getEncryptionSalt());
        String newSalt = generateSalt();
        String newKey = deriveEncryptionKey(req.getNewPassword(), newSalt);

        // Call client-service to re-encrypt all personal data
        reEncryptClientData(user.getClientId(), oldKey, newKey);

        // Update auth credentials
        user.setPasswordHash(encoder.encode(req.getNewPassword()));
        user.setEncryptionSalt(newSalt);
        userRepo.save(user);

        // Revoke all existing refresh tokens (force re-login with new key)
        refreshTokenService.revokeAllUserTokens(user);

        log.info("Password changed and data re-encrypted for user: {}", username);
    }

    // ======================== LOGOUT ========================

    public void logout(String refreshTokenValue) {
        try {
            refreshTokenService.revokeRefreshToken(refreshTokenValue);
        } catch (Exception e) {
            // Log and continue
        }
    }

    // ======================== ENCRYPTION KEY DERIVATION ========================

    /**
     * Derives an AES-256 encryption key from the user's password + per-user salt.
     * Uses PBKDF2WithHmacSHA256, same algorithm as EncryptionService in client-service.
     * The derived key is returned as a Base64-encoded string.
     */
    public String deriveEncryptionKey(String password, String saltBase64) {
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            SecretKey key = factory.generateSecret(spec);
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive encryption key", e);
        }
    }

    /**
     * Generates a random 24-byte salt, returned as Base64.
     */
    private String generateSalt() {
        byte[] salt = new byte[24];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    // ======================== INTER-SERVICE: RE-ENCRYPTION / MIGRATION ========================

    /**
     * Migrates client rows still encrypted with the legacy server key (sign-up / old DB) to the user-derived key.
     */
    public void runPostLoginClientEncryption(Long clientId, String encryptionKey) {
        if (encryptionKey == null || encryptionKey.isBlank()) {
            return;
        }
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Api-Secret", internalApiSecret);

            Map<String, Object> body = Map.of(
                    "clientId", clientId,
                    "newEncryptionKey", encryptionKey
            );

            restTemplate.postForEntity(
                    clientServiceUrl + "/api/internal/clients/migrate-legacy",
                    new HttpEntity<>(body, headers),
                    Void.class
            );
        } catch (Exception e) {
            log.warn("Legacy migration failed for clientId={}: {}", clientId, e.getMessage());
        }
    }

    private void reEncryptClientData(Long clientId, String oldKey, String newKey) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Api-Secret", internalApiSecret);

            Map<String, Object> body = Map.of(
                    "clientId", clientId,
                    "oldEncryptionKey", oldKey,
                    "newEncryptionKey", newKey
            );

            restTemplate.postForEntity(
                    clientServiceUrl + "/api/internal/clients/re-encrypt",
                    new HttpEntity<>(body, headers),
                    Void.class
            );
        } catch (Exception e) {
            log.error("Failed to re-encrypt client data for clientId={}: {}", clientId, e.getMessage());
            throw new RuntimeException("Password change failed: could not re-encrypt personal data. Please try again.", e);
        }
    }

    // ======================== PASSWORD VALIDATION ========================

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Minimum 8 characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Must contain uppercase letter");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("Must contain number");
        }
        if (!password.matches(".*[!@#$%^&*].*")) {
            throw new IllegalArgumentException("Must contain special character");
        }
    }
}
