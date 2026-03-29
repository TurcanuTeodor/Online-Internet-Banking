package ro.app.client.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ro.app.client.internal.InternalApiHeaders;
import ro.app.client.service.ClientService;

/**
 * Internal API for service-to-service communication.
 * Secured by shared secret header, not JWT (since calling service is auth-service, not a user).
 */
@RestController
@RequestMapping("/api/internal/clients")
public class InternalClientController {

    private static final Logger log = LoggerFactory.getLogger(InternalClientController.class);

    private final ClientService clientService;

    @Value("${app.internal.api-secret:change-me-internal-secret}")
    private String expectedSecret;

    public InternalClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    /**
     * Re-encrypt all personal data when user changes password.
     * Called by auth-service during password change flow.
     */
    @PostMapping("/re-encrypt")
    public ResponseEntity<?> reEncryptClientData(
            @RequestHeader(InternalApiHeaders.SECRET) String secret,
            @RequestBody Map<String, Object> body) {

        if (!expectedSecret.equals(secret)) {
            log.warn("Invalid internal API secret received for re-encrypt request");
            return ResponseEntity.status(403).body(Map.of("error", "Invalid internal API secret"));
        }

        Long clientId = ((Number) body.get("clientId")).longValue();
        String oldKey = (String) body.get("oldEncryptionKey");
        String newKey = (String) body.get("newEncryptionKey");

        log.info("Re-encrypting data for clientId={}", clientId);
        clientService.reEncryptClientData(clientId, oldKey, newKey);
        log.info("Successfully re-encrypted data for clientId={}", clientId);

        return ResponseEntity.ok(Map.of("message", "Data re-encrypted successfully"));
    }

    /**
     * Migrates ciphertext from legacy {@code ENCRYPTION_KEY} to user-derived key after login/register.
     */
    @PostMapping("/migrate-legacy")
    public ResponseEntity<?> migrateLegacy(
            @RequestHeader(InternalApiHeaders.SECRET) String secret,
            @RequestBody Map<String, Object> body) {

        if (!expectedSecret.equals(secret)) {
            log.warn("Invalid internal API secret for migrate-legacy");
            return ResponseEntity.status(403).body(Map.of("error", "Invalid internal API secret"));
        }

        Long clientId = ((Number) body.get("clientId")).longValue();
        String newKey = (String) body.get("newEncryptionKey");

        log.info("Migrate legacy encryption if needed for clientId={}", clientId);
        clientService.migrateLegacyEncryption(clientId, newKey);
        return ResponseEntity.ok(Map.of("message", "OK"));
    }
}
