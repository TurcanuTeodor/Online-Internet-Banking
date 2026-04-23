package ro.app.client.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ClientKeyResolverTest {

    @Test
    void resolveKey_UsesProvidedKey_WhenPresent() {
        KeyManagementProvider provider = Mockito.mock(KeyManagementProvider.class);
        when(provider.activeKey()).thenReturn("fallback-key");
        when(provider.previousKey()).thenReturn("fallback-old");
        when(provider.activeKeyVersion()).thenReturn("v2");

        ClientKeyResolver resolver = new ClientKeyResolver(provider);
        assertEquals("user-key", resolver.resolveKey("user-key"));
    }

    @Test
    void resolveKey_UsesFallback_WhenBlankOrNull() {
        KeyManagementProvider provider = Mockito.mock(KeyManagementProvider.class);
        when(provider.activeKey()).thenReturn("fallback-key");
        when(provider.previousKey()).thenReturn("fallback-old");
        when(provider.activeKeyVersion()).thenReturn("v2");

        ClientKeyResolver resolver = new ClientKeyResolver(provider);
        assertEquals("fallback-key", resolver.resolveKey(""));
        assertEquals("fallback-key", resolver.resolveKey(null));
        assertEquals("fallback-key", resolver.fallbackKey());
        assertEquals("fallback-old", resolver.previousFallbackKey());
        assertEquals("v2", resolver.activeFallbackKeyVersion());
    }
}
