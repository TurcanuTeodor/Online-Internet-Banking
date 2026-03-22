package ro.app.client.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.validation.constraints.NotNull;
import ro.app.client.dto.gdpr.ClientExportDTO;
import ro.app.client.exception.ResourceNotFoundException;
import ro.app.client.model.entity.Client;
import ro.app.client.model.entity.ContactInfo;
import ro.app.client.repository.ClientRepository;
import ro.app.client.repository.ContactInfoRepository;

/**
 * GDPR export — decrypts client and contact data for portability / access requests.
 */
@Service
public class ClientGdprService {

    private final ClientRepository clientRepository;
    private final ContactInfoRepository contactInfoRepository;
    private final EncryptionService encryptionService;
    private final String encryptionKey;

    public ClientGdprService(
            ClientRepository clientRepository,
            ContactInfoRepository contactInfoRepository,
            EncryptionService encryptionService,
            @Value("${encryption.key}") String encryptionKey) {
        this.clientRepository = clientRepository;
        this.contactInfoRepository = contactInfoRepository;
        this.encryptionService = encryptionService;
        this.encryptionKey = encryptionKey;
    }

    public ClientExportDTO exportClientData(@NotNull Long clientId) {
        if (clientId == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        ContactInfo contactInfo = contactInfoRepository.findByClientId(clientId);

        ClientExportDTO export = new ClientExportDTO();

        export.setClientId(client.getId());
        try {
            export.setFirstName(encryptionService.decrypt(client.getFirstName(), encryptionKey));
            export.setLastName(encryptionService.decrypt(client.getLastName(), encryptionKey));
        } catch (Exception e) {
            export.setFirstName(client.getFirstName());
            export.setLastName(client.getLastName());
        }
        export.setClientType(client.getClientType() != null ? client.getClientType().getCode() : null);
        export.setSexType(client.getSexType() != null ? client.getSexType().getCode() : null);
        export.setRiskLevel(client.getRiskLevel());
        export.setActive(client.isActive());

        if (contactInfo != null) {
            try {
                export.setEmail(encryptionService.decrypt(contactInfo.getEmail(), encryptionKey));
                export.setPhone(encryptionService.decrypt(contactInfo.getPhone(), encryptionKey));
                export.setContactPerson(encryptionService.decrypt(contactInfo.getContactPerson(), encryptionKey));
                export.setWebsite(encryptionService.decrypt(contactInfo.getWebsite(), encryptionKey));
                export.setAddress(encryptionService.decrypt(contactInfo.getAddress(), encryptionKey));
                export.setCity(encryptionService.decrypt(contactInfo.getCity(), encryptionKey));
                export.setPostalCode(encryptionService.decrypt(contactInfo.getPostalCode(), encryptionKey));
            } catch (Exception e) {
                export.setEmail(contactInfo.getEmail());
                export.setPhone(contactInfo.getPhone());
                export.setContactPerson(contactInfo.getContactPerson());
                export.setWebsite(contactInfo.getWebsite());
                export.setAddress(contactInfo.getAddress());
                export.setCity(contactInfo.getCity());
                export.setPostalCode(contactInfo.getPostalCode());
            }
        }

        return export;
    }
}
