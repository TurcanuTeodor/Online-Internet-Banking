package ro.app.client.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.security.key-management.mode", havingValue = "env", matchIfMissing = true)
public class EnvKeyManagementProvider implements KeyManagementProvider {

    private final String activeKey;
    private final String previousKey;
    private final String activeKeyVersion;

    public EnvKeyManagementProvider(
            @Value("${encryption.key}") String activeKey,
            @Value("${encryption.key.previous:}") String previousKey,
            @Value("${encryption.key.version:v1}") String activeKeyVersion) {
        this.activeKey = activeKey;
        this.previousKey = previousKey;
        this.activeKeyVersion = activeKeyVersion;
    }

    @Override
    public String activeKey() {
        return activeKey;
    }

    @Override
    public String previousKey() {
        return previousKey;
    }

    @Override
    public String activeKeyVersion() {
        return activeKeyVersion;
    }
}
