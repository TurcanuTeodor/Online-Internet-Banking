package ro.app.client.service;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Teste unitare pentru ClientKeyResolver (JUnit 4).
 *
 * Acopera logica de selectie a cheii de criptare:
 *   1. Cheia furnizata de utilizator — folosita daca nu este nula/vida
 *   2. Cheia fallback din KMS — folosita daca cheia utilizatorului lipseste
 *
 * Pattern testat: Strategy (Behavioral) — selectarea cheii in functie de context.
 */
@RunWith(MockitoJUnitRunner.class)
public class ClientKeyResolverTest {

    private ClientKeyResolver buildResolver() {
        KeyManagementProvider provider = Mockito.mock(KeyManagementProvider.class);
        Mockito.when(provider.activeKey()).thenReturn("fallback-key");
        Mockito.when(provider.previousKey()).thenReturn("fallback-old");
        Mockito.when(provider.activeKeyVersion()).thenReturn("v2");
        return new ClientKeyResolver(provider);
    }

    @Test
    public void resolveKey_userKeyProvided_returnsUserKey() {
        // Arrange
        ClientKeyResolver resolver = buildResolver();

        // Act
        String resolved = resolver.resolveKey("user-key");

        // Assert — cheia furnizata explicit are prioritate
        assertEquals("user-key", resolved);
    }

    @Test
    public void resolveKey_emptyUserKey_returnsActiveFallback() {
        // Arrange — Boundary: sir vid => fallback
        ClientKeyResolver resolver = buildResolver();

        // Act
        String resolved = resolver.resolveKey("");

        // Assert
        assertEquals("fallback-key", resolved);
    }

    @Test
    public void resolveKey_nullUserKey_returnsActiveFallback() {
        // Arrange — Boundary: null => fallback
        ClientKeyResolver resolver = buildResolver();

        // Act
        String resolved = resolver.resolveKey(null);

        // Assert
        assertEquals("fallback-key", resolved);
    }

    @Test
    public void fallbackKey_returnsDelegatedActiveKey() {
        ClientKeyResolver resolver = buildResolver();
        assertEquals("fallback-key", resolver.fallbackKey());
    }

    @Test
    public void previousFallbackKey_returnsDelegatedPreviousKey() {
        ClientKeyResolver resolver = buildResolver();
        assertEquals("fallback-old", resolver.previousFallbackKey());
    }

    @Test
    public void activeFallbackKeyVersion_returnsVersion() {
        ClientKeyResolver resolver = buildResolver();
        assertEquals("v2", resolver.activeFallbackKeyVersion());
    }
}
