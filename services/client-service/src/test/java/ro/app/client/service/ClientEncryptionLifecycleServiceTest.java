package ro.app.client.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import ro.app.client.model.entity.Client;
import ro.app.client.model.enums.ClientType;
import ro.app.client.model.enums.SexType;
import ro.app.client.repository.ClientRepository;
import ro.app.client.repository.ContactInfoRepository;

@ExtendWith(MockitoExtension.class)
class ClientEncryptionLifecycleServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ContactInfoRepository contactInfoRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private ClientKeyResolver keyResolver;

    private ClientEncryptionLifecycleService service;

    @BeforeEach
    void setUp() {
        service = new ClientEncryptionLifecycleService(
                clientRepository,
                contactInfoRepository,
                encryptionService,
                keyResolver);
    }

    @Test
    void migrateLegacyEncryption_NoMigration_WhenAlreadyEncryptedWithNewKey() throws Exception {
        Long clientId = 1L;
        String newKey = "new-key";
        Client client = encryptedClient(clientId, "enc-first", "enc-last");

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(encryptionService.decrypt("enc-first", newKey)).thenReturn("First");

        service.migrateLegacyEncryption(clientId, newKey);

        verify(clientRepository, times(1)).findById(clientId);
        verify(encryptionService, times(1)).decrypt("enc-first", newKey);
        verify(clientRepository, never()).save(any(Client.class));
        verify(contactInfoRepository, never()).findByClientId(any());
    }

    @Test
    void migrateLegacyEncryption_ReEncryptsUsingActiveFallbackKey() throws Exception {
        Long clientId = 2L;
        String newKey = "new-key";
        String activeFallback = "active-old";

        Client client = encryptedClient(clientId, "enc-first", "enc-last");
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

        service.migrateLegacyEncryption(clientId, newKey);

        verify(clientRepository).save(client);
        assertEquals("new-enc-first", client.getFirstName());
        assertEquals("new-enc-last", client.getLastName());
    }

    @Test
    void migrateLegacyEncryption_ReEncryptsUsingPreviousFallback_WhenActiveFails() throws Exception {
        Long clientId = 3L;
        String newKey = "new-key";
        String activeFallback = "active-old";
        String previousFallback = "previous-old";

        Client client = encryptedClient(clientId, "enc-first", "enc-last");
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

        service.migrateLegacyEncryption(clientId, newKey);

        verify(encryptionService).decrypt("enc-first", previousFallback);
        verify(clientRepository).save(client);
        assertEquals("new-ana", client.getFirstName());
        assertEquals("new-pop", client.getLastName());
    }

    @Test
    void migrateLegacyEncryption_NoMigration_WhenNoCandidateCanDecrypt() throws Exception {
        Long clientId = 4L;
        String newKey = "new-key";
        String activeFallback = "active-old";
        String previousFallback = "previous-old";

        Client client = encryptedClient(clientId, "enc-first", "enc-last");
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(keyResolver.fallbackKey()).thenReturn(activeFallback);
        when(keyResolver.previousFallbackKey()).thenReturn(previousFallback);

        when(encryptionService.decrypt("enc-first", newKey)).thenThrow(new RuntimeException("not new"));
        when(encryptionService.decrypt("enc-first", activeFallback)).thenThrow(new RuntimeException("active failed"));
        when(encryptionService.decrypt("enc-first", previousFallback)).thenThrow(new RuntimeException("previous failed"));

        service.migrateLegacyEncryption(clientId, newKey);

        verify(clientRepository, never()).save(any(Client.class));
        verify(contactInfoRepository, never()).findByClientId(any());
        verify(encryptionService, never()).encrypt(any(), eq(newKey));
    }

    private static Client encryptedClient(Long id, String firstName, String lastName) {
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
