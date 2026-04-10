package ro.app.client.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class ClientKeyResolverTest {

    @Test
    void resolveKey_UsesProvidedKey_WhenPresent() {
        ClientKeyResolver resolver = new ClientKeyResolver("fallback-key");
        assertEquals("user-key", resolver.resolveKey("user-key"));
    }

    @Test
    void resolveKey_UsesFallback_WhenBlankOrNull() {
        ClientKeyResolver resolver = new ClientKeyResolver("fallback-key");
        assertEquals("fallback-key", resolver.resolveKey(""));
        assertEquals("fallback-key", resolver.resolveKey(null));
        assertEquals("fallback-key", resolver.fallbackKey());
    }
}
