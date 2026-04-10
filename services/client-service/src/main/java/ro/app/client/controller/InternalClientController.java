package ro.app.client.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ro.app.client.internal.InternalApiHeaders;
import ro.app.client.internal.MigrateLegacyRequest;
import ro.app.client.internal.ReEncryptClientRequest;
import ro.app.client.service.ClientEncryptionLifecycleService;

/**
 * Internal API for service-to-service communication.
 * Secured by shared secret header, not JWT (since calling service is auth-service, not a user).
 */
@RestController
@RequestMapping("/api/internal/clients")
public class InternalClientController {

    private static final Logger log = LoggerFactory.getLogger(InternalClientController.class);

    private final ClientEncryptionLifecycleService clientEncryptionLifecycleService;

    @Value("${app.internal.api-secret:change-me-internal-secret}")
    private String expectedSecret;

    public InternalClientController(ClientEncryptionLifecycleService clientEncryptionLifecycleService) {
        this.clientEncryptionLifecycleService = clientEncryptionLifecycleService;
    }

    /**
     * Re-encrypt all personal data when user changes password.
     * Called by auth-service during password change flow.
     */
    @PostMapping("/re-encrypt")
    public ResponseEntity<?> reEncryptClientData(
            @RequestHeader(InternalApiHeaders.SECRET) String secret,
            @Valid @RequestBody ReEncryptClientRequest body) {

        if (!MessageDigest.isEqual(
                expectedSecret.getBytes(StandardCharsets.UTF_8),
                secret.getBytes(StandardCharsets.UTF_8))) {
            log.warn("Invalid internal API secret received for re-encrypt request");
            return ResponseEntity.status(403).body(Map.of("error", "Invalid internal API secret"));
        }

        Long clientId = body.clientId();
        String oldKey = body.oldEncryptionKey();
        String newKey = body.newEncryptionKey();

        log.info("Re-encrypting data for clientId={}", clientId);
        clientEncryptionLifecycleService.reEncryptClientData(clientId, oldKey, newKey);
        log.info("Successfully re-encrypted data for clientId={}", clientId);

        return ResponseEntity.ok(Map.of("message", "Data re-encrypted successfully"));
    }

    /**
     * Migrates ciphertext from legacy {@code ENCRYPTION_KEY} to user-derived key after login/register.
     */
    @PostMapping("/migrate-legacy")
    public ResponseEntity<?> migrateLegacy(
            @RequestHeader(InternalApiHeaders.SECRET) String secret,
            @Valid @RequestBody MigrateLegacyRequest body) {

        if (!MessageDigest.isEqual(
                expectedSecret.getBytes(StandardCharsets.UTF_8),
                secret.getBytes(StandardCharsets.UTF_8))) {
            log.warn("Invalid internal API secret for migrate-legacy");
            return ResponseEntity.status(403).body(Map.of("error", "Invalid internal API secret"));
        }

        Long clientId = body.clientId();
        String newKey = body.newEncryptionKey();

        log.info("Migrate legacy encryption if needed for clientId={}", clientId);
        clientEncryptionLifecycleService.migrateLegacyEncryption(clientId, newKey);
        return ResponseEntity.ok(Map.of("message", "OK"));
    }
}
