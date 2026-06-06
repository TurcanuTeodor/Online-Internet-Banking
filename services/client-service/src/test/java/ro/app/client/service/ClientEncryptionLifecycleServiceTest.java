package ro.app.client.service;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.MockitoJUnitRunner;

import ro.app.client.model.entity.Client;
import ro.app.client.model.enums.ClientType;
import ro.app.client.model.enums.SexType;
import ro.app.client.repository.ClientRepository;
import ro.app.client.repository.ContactInfoRepository;

/**
 * Teste unitare pentru ClientEncryptionLifecycleService (JUnit 4).
 *
 * Acopera cele 4 scenarii de migrare a criptarii:
 *   1. Clientul deja criptat cu cheia noua — fara migrare
 *   2. Cheie activa fallback functioneaza — re-cripteaza
 *   3. Cheie anterioara fallback functioneaza — re-cripteaza
 *   4. Nicio cheie nu functioneaza — nu salveaza nimic
 *
 * Pattern testat: Strategy (Behavioral) — selectarea cheii de decriptare
 * in functie de rezultatul incercarii.
 */
@RunWith(MockitoJUnitRunner.class)
public class ClientEncryptionLifecycleServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ContactInfoRepository contactInfoRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private ClientKeyResolver keyResolver;

    private ClientEncryptionLifecycleService service;

    @Before
    public void setUp() {
        service = new ClientEncryptionLifecycleService(
                clientRepository,
                contactInfoRepository,
                encryptionService,
                keyResolver);
    }

    // ── Scenario 1: deja criptat cu cheia noua — fara migrare ─────────────────

    @Test
    public void migrateLegacyEncryption_alreadyEncryptedWithNewKey_noSave() throws Exception {
        // Arrange
        Long clientId = 1L;
        String newKey = "new-key";
        Client client = buildEncryptedClient(clientId, "enc-first", "enc-last");

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(encryptionService.decrypt("enc-first", newKey)).thenReturn("First");

        // Act
        service.migrateLegacyEncryption(clientId, newKey);

        // Assert — nu trebuie sa salveze nimic
        verify(clientRepository, times(1)).findById(clientId);
        verify(encryptionService, times(1)).decrypt("enc-first", newKey);
        verify(clientRepository, never()).save(Mockito.any(Client.class));
        verify(contactInfoRepository, never()).findByClientId(Mockito.any());
    }

    // ── Scenario 2: re-cripteaza cu cheia activa fallback ─────────────────────

    @Test
    public void migrateLegacyEncryption_activeFallbackWorks_reEncryptsAndSaves() throws Exception {
        // Arrange
        Long clientId = 2L;
        String newKey = "new-key";
        String activeFallback = "active-old";

        Client client = buildEncryptedClient(clientId, "enc-first", "enc-last");
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(keyResolver.fallbackKey()).thenReturn(activeFallback);
        when(keyResolver.previousFallbackKey()).thenReturn("previous-old");

        when(encryptionService.decrypt("enc-first", newKey)).thenThrow(new RuntimeException("not new"));
        when(encryptionService.decrypt("enc-first", activeFallback)).thenReturn("First");

        when(encryptionService.decryptFlexible("enc-first", activeFallback, activeFallback)).thenReturn("John");
        when(encryptionService.decryptFlexible("enc-last", activeFallback, activeFallback)).thenReturn("Doe");
        when(encryptionService.encrypt("John", newKey)).thenReturn("new-enc-first");
        when(encryptionService.encrypt("Doe", newKey)).thenReturn("new-enc-last");
        when(contactInfoRepository.findByClientId(clientId)).thenReturn(null);

        // Act
        service.migrateLegacyEncryption(clientId, newKey);

        // Assert
        verify(clientRepository).save(client);
        assertEquals("new-enc-first", client.getFirstName());
        assertEquals("new-enc-last", client.getLastName());
    }

    // ── Scenario 3: cheia activa esueaza, cheia anterioara functioneaza ────────

    @Test
    public void migrateLegacyEncryption_previousFallbackWorks_reEncryptsAndSaves() throws Exception {
        // Arrange
        Long clientId = 3L;
        String newKey = "new-key";
        String activeFallback = "active-old";
        String previousFallback = "previous-old";

        Client client = buildEncryptedClient(clientId, "enc-first", "enc-last");
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(keyResolver.fallbackKey()).thenReturn(activeFallback);
        when(keyResolver.previousFallbackKey()).thenReturn(previousFallback);

        when(encryptionService.decrypt("enc-first", newKey)).thenThrow(new RuntimeException("not new"));
        when(encryptionService.decrypt("enc-first", activeFallback)).thenThrow(new RuntimeException("active failed"));
        when(encryptionService.decrypt("enc-first", previousFallback)).thenReturn("First");

        when(encryptionService.decryptFlexible("enc-first", previousFallback, activeFallback)).thenReturn("Ana");
        when(encryptionService.decryptFlexible("enc-last", previousFallback, activeFallback)).thenReturn("Pop");
        when(encryptionService.encrypt("Ana", newKey)).thenReturn("new-ana");
        when(encryptionService.encrypt("Pop", newKey)).thenReturn("new-pop");
        when(contactInfoRepository.findByClientId(clientId)).thenReturn(null);

        // Act
        service.migrateLegacyEncryption(clientId, newKey);

        // Assert
        verify(encryptionService).decrypt("enc-first", previousFallback);
        verify(clientRepository).save(client);
        assertEquals("new-ana", client.getFirstName());
        assertEquals("new-pop", client.getLastName());
    }

    // ── Scenario 4: nicio cheie nu functioneaza — fara salvare ────────────────

    @Test
    public void migrateLegacyEncryption_noKeyWorks_noSave() throws Exception {
        // Arrange
        Long clientId = 4L;
        String newKey = "new-key";
        String activeFallback = "active-old";
        String previousFallback = "previous-old";

        Client client = buildEncryptedClient(clientId, "enc-first", "enc-last");
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(keyResolver.fallbackKey()).thenReturn(activeFallback);
        when(keyResolver.previousFallbackKey()).thenReturn(previousFallback);

        when(encryptionService.decrypt("enc-first", newKey)).thenThrow(new RuntimeException("not new"));
        when(encryptionService.decrypt("enc-first", activeFallback)).thenThrow(new RuntimeException("active failed"));
        when(encryptionService.decrypt("enc-first", previousFallback)).thenThrow(new RuntimeException("previous failed"));

        // Act
        service.migrateLegacyEncryption(clientId, newKey);

        // Assert — nicio salvare, nicio incercare de re-criptare
        verify(clientRepository, never()).save(Mockito.any(Client.class));
        verify(contactInfoRepository, never()).findByClientId(Mockito.any());
        verify(encryptionService, never()).encrypt(Mockito.any(), Mockito.eq(newKey));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Client buildEncryptedClient(Long id, String firstName, String lastName) {
        Client client = new Client();
        client.setId(id);
        client.setFirstName(firstName);
        client.setLastName(lastName);
        client.setClientType(ClientType.PF);
        client.setSexType(SexType.M);
        client.setActive(true);
        return client;
    }
}
