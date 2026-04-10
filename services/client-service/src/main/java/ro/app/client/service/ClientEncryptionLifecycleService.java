package ro.app.client.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import ro.app.client.exception.ResourceNotFoundException;
import ro.app.client.model.entity.Client;
import ro.app.client.model.entity.ContactInfo;
import ro.app.client.repository.ClientRepository;
import ro.app.client.repository.ContactInfoRepository;

@Service
public class ClientEncryptionLifecycleService {

    private final ClientRepository clientRepository;
    private final ContactInfoRepository contactInfoRepository;
    private final EncryptionService encryptionService;
    private final ClientKeyResolver keyResolver;

    public ClientEncryptionLifecycleService(
            ClientRepository clientRepository,
            ContactInfoRepository contactInfoRepository,
            EncryptionService encryptionService,
            ClientKeyResolver keyResolver) {
        this.clientRepository = clientRepository;
        this.contactInfoRepository = contactInfoRepository;
        this.encryptionService = encryptionService;
        this.keyResolver = keyResolver;
    }

    @Transactional
    public void reEncryptClientData(Long clientId, String oldKey, String newKey) {
        if (clientId == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found for re-encryption"));

        try {
            String decryptedFirst = safeDecrypt(client.getFirstName(), oldKey);
            String decryptedLast = safeDecrypt(client.getLastName(), oldKey);
            client.setFirstName(encryptionService.encrypt(decryptedFirst, newKey));
            client.setLastName(encryptionService.encrypt(decryptedLast, newKey));
            clientRepository.save(client);
        } catch (Exception e) {
            throw new RuntimeException("Failed to re-encrypt client names for clientId=" + clientId, e);
        }

        ContactInfo contactInfo = contactInfoRepository.findByClientId(clientId);
        if (contactInfo != null) {
            try {
                contactInfo.setPhone(reEncryptField(contactInfo.getPhone(), oldKey, newKey));
                contactInfo.setEmail(reEncryptField(contactInfo.getEmail(), oldKey, newKey));
                contactInfo.setContactPerson(reEncryptField(contactInfo.getContactPerson(), oldKey, newKey));
                contactInfo.setWebsite(reEncryptField(contactInfo.getWebsite(), oldKey, newKey));
                contactInfo.setAddress(reEncryptField(contactInfo.getAddress(), oldKey, newKey));
                contactInfo.setCity(reEncryptField(contactInfo.getCity(), oldKey, newKey));
                contactInfo.setPostalCode(reEncryptField(contactInfo.getPostalCode(), oldKey, newKey));
                contactInfoRepository.save(contactInfo);
            } catch (Exception e) {
                throw new RuntimeException("Failed to re-encrypt contact info for clientId=" + clientId, e);
            }
        }
    }

    @Transactional
    public void migrateLegacyEncryption(Long clientId, String newKey) {
        if (clientId == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }
        if (newKey == null || newKey.isBlank()) {
            return;
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        try {
            encryptionService.decrypt(client.getFirstName(), newKey);
            return;
        } catch (Exception ignored) {
        }

        try {
            encryptionService.decrypt(client.getFirstName(), keyResolver.fallbackKey());
        } catch (Exception e) {
            return;
        }

        reEncryptClientData(clientId, keyResolver.fallbackKey(), newKey);
    }

    private String reEncryptField(String encryptedValue, String oldKey, String newKey) throws Exception {
        if (encryptedValue == null || encryptedValue.isBlank()) {
            return encryptedValue;
        }
        String decrypted = safeDecrypt(encryptedValue, oldKey);
        return encryptionService.encrypt(decrypted, newKey);
    }

    private String safeDecrypt(String encryptedValue, String key) {
        if (encryptedValue == null || encryptedValue.isBlank()) {
            return encryptedValue;
        }
        try {
            return encryptionService.decryptFlexible(encryptedValue, key, keyResolver.fallbackKey());
        } catch (Exception e) {
            return encryptedValue;
        }
    }
}