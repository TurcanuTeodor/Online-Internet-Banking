package ro.app.client.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.app.client.exception.ResourceNotFoundException;
import ro.app.client.model.entity.Client;
import ro.app.client.repository.ClientRepository;
import ro.app.client.repository.ContactInfoRepository;

/**
 * Local DB changes for GDPR right to erasure (client row + contact info).
 */
@Service
public class ClientGdprErasurePersistenceService {

    private static final String DELETED_PLACEHOLDER = "DELETED";

    private final ClientRepository clientRepository;
    private final ContactInfoRepository contactInfoRepository;
    private final EncryptionService encryptionService;
    private final String encryptionKey;

    public ClientGdprErasurePersistenceService(
            ClientRepository clientRepository,
            ContactInfoRepository contactInfoRepository,
            EncryptionService encryptionService,
            @Value("${encryption.key}") String encryptionKey) {
        this.clientRepository = clientRepository;
        this.contactInfoRepository = contactInfoRepository;
        this.encryptionService = encryptionService;
        this.encryptionKey = encryptionKey;
    }

    @Transactional
    public void applyLocalErasure(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        contactInfoRepository.deleteAllForClientId(clientId);

        try {
            client.setFirstName(encryptionService.encrypt(DELETED_PLACEHOLDER, encryptionKey));
            client.setLastName(encryptionService.encrypt(DELETED_PLACEHOLDER, encryptionKey));
        } catch (Exception e) {
            client.setFirstName(DELETED_PLACEHOLDER);
            client.setLastName(DELETED_PLACEHOLDER);
        }

        client.setActive(false);
        clientRepository.save(client);
    }
}
