package ro.app.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ro.app.auth.config.AuthProperties;

/**
 * AES-256-GCM encryption for service-level secrets (e.g. TOTP twoFactorSecret).
 *
 * <p>
 * Uses the same algorithm as client-service EncryptionService:
 * PBKDF2WithHmacSHA256 key derivation + AES/GCM/NoPadding encryption.
 * Ciphertext format: {@code Base64(salt):Base64(iv):Base64(ciphertext)}
 *
 * <p>
 * Key source: {@code SERVICE_ENCRYPTION_KEY} environment variable,
 * bound via {@code app.auth.security.service-key} in AuthProperties.
 *
 * <p>
 * <b>Migration</b>: {@link #decryptOrLegacy(String)} handles legacy rows
 * where the secret is still stored as plaintext (pre-encryption rollout).
 */
@Service
public class ServiceEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(ServiceEncryptionService.class);

    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    private final String serviceKey;

    public ServiceEncryptionService(AuthProperties authProperties) {
        this.serviceKey = authProperties.getSecurity().getServiceKey();
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException(
                    "SERVICE_ENCRYPTION_KEY is not configured. " +
                            "Set app.auth.security.service-key in application.properties " +
                            "and provide the SERVICE_ENCRYPTION_KEY environment variable.");
        }
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Encrypts {@code plaintext} using AES-256-GCM with a random salt and IV.
     *
     * @return ciphertext in format {@code salt:iv:ciphertext} (all Base64)
     */
    public String encrypt(String plaintext) {
        try {
            byte[] salt = randomBytes(SALT_LENGTH);
            byte[] iv = randomBytes(IV_LENGTH);
            SecretKey key = deriveKey(serviceKey, salt);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return b64(salt) + ":" + b64(iv) + ":" + b64(cipherBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt service secret", e);
        }
    }

    /**
     * Decrypts a ciphertext produced by {@link #encrypt(String)}.
     *
     * @throws RuntimeException if decryption fails (wrong key or corrupted data)
     */
    public String decrypt(String ciphertext) {
        try {
            String[] parts = ciphertext.split(":");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid ciphertext format — expected salt:iv:data");
            }
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] cipherBytes = Base64.getDecoder().decode(parts[2]);

            SecretKey key = deriveKey(serviceKey, salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));

            return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt service secret", e);
        }
    }

    /**
     * Migration-safe decrypt: tries AES-GCM decryption first.
     * If it fails (legacy plaintext row from before encryption was introduced),
     * returns the value as-is and logs a migration warning.
     *
     * <p>
     * Call sites should re-encrypt plaintext values on first successful use.
     *
     * @param value the stored value — either encrypted ciphertext or legacy
     *              plaintext
     * @return the decrypted secret, or the original {@code value} if it is
     *         plaintext legacy
     */
    public String decryptOrLegacy(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        // Encrypted format always has exactly 2 ':' separators
        if (!value.contains(":")) {
            log.warn("TOTP secret appears to be legacy plaintext — migration pending for this user");
            return value;
        }
        try {
            return decrypt(value);
        } catch (Exception e) {
            log.warn("TOTP secret decryption failed, treating as legacy plaintext: {}", e.getMessage());
            return value;
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private SecretKey deriveKey(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    private String b64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
