package ro.app.client.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Production extension point for secret-manager/KMS integration.
 *
 * This baseline intentionally does not bind to a vendor SDK; wire KMS client here
 * (AWS KMS, Azure Key Vault etc.).
 */
@Component
@ConditionalOnProperty(name = "app.security.key-management.mode", havingValue = "kms")
public class KmsKeyManagementProvider implements KeyManagementProvider {

    private final String activeKeyUri;
    private final String previousKeyUri;
    private final String activeKeyVersion;

    public KmsKeyManagementProvider(
            @Value("${app.security.kms.active-key-uri:}") String activeKeyUri,
            @Value("${app.security.kms.previous-key-uri:}") String previousKeyUri,
            @Value("${app.security.kms.active-key-version:v1}") String activeKeyVersion) {
        this.activeKeyUri = activeKeyUri;
        this.previousKeyUri = previousKeyUri;
        this.activeKeyVersion = activeKeyVersion;
    }

    @Override
    public String activeKey() {
        throw new IllegalStateException(
                "KMS mode is enabled but no KMS client is implemented. Configure a provider in KmsKeyManagementProvider for "
                        + activeKeyUri);
    }

    @Override
    public String previousKey() {
        return previousKeyUri;
    }

    @Override
    public String activeKeyVersion() {
        return activeKeyVersion;
    }
}
