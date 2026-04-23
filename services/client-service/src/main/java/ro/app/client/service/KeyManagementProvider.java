package ro.app.client.service;

/**
 * Central abstraction for encryption key retrieval.
 *
 * Local/demo uses env-backed keys; production can switch to a KMS-backed implementation.
 */
public interface KeyManagementProvider {

    /**
     * Returns the active fallback key for server-side encryption/decryption.
     */
    String activeKey();

    /**
     * Returns the previous fallback key used during key rotation (optional).
     */
    String previousKey();

    /**
     * Logical version/label for the active key (for audit/ops visibility).
     */
    String activeKeyVersion();
}
