package ro.app.client.service;

import org.springframework.stereotype.Component;

@Component
public class ClientKeyResolver {

    private final KeyManagementProvider keyManagementProvider;

    public ClientKeyResolver(KeyManagementProvider keyManagementProvider) {
        this.keyManagementProvider = keyManagementProvider;
    }

    public String resolveKey(String encryptionKey) {
        return (encryptionKey != null && !encryptionKey.isBlank()) ? encryptionKey : keyManagementProvider.activeKey();
    }

    public String fallbackKey() {
        return keyManagementProvider.activeKey();
    }

    public String previousFallbackKey() {
        return keyManagementProvider.previousKey();
    }

    public String activeFallbackKeyVersion() {
        return keyManagementProvider.activeKeyVersion();
    }
}