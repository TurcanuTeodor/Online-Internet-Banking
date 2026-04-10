package ro.app.client.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClientKeyResolver {

    private final String fallbackEncryptionKey;

    public ClientKeyResolver(@Value("${encryption.key}") String fallbackEncryptionKey) {
        this.fallbackEncryptionKey = fallbackEncryptionKey;
    }

    public String resolveKey(String encryptionKey) {
        return (encryptionKey != null && !encryptionKey.isBlank()) ? encryptionKey : fallbackEncryptionKey;
    }

    public String fallbackKey() {
        return fallbackEncryptionKey;
    }
}