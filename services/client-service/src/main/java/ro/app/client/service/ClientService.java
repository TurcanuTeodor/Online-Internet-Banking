package ro.app.client.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import ro.app.client.dto.ClientDTO;
import ro.app.client.dto.ContactInfoDTO;
import ro.app.client.dto.ViewClientDTO;
import ro.app.client.dto.mapper.ClientMapper;
import ro.app.client.dto.mapper.ContactInfoMapper;
import ro.app.client.exception.ResourceNotFoundException;
import ro.app.client.model.entity.ContactInfo;
import ro.app.client.model.entity.Client;
import ro.app.client.model.enums.ClientType;
import ro.app.client.model.enums.SexType;
import ro.app.client.model.view.ViewClient;
import ro.app.client.repository.ClientRepository;
import ro.app.client.repository.ContactInfoRepository;
import ro.app.client.repository.ViewClientRepository;

@Service
public class ClientService {

    private static final String PII_MASK = "[PROTECTED]";

    private final ClientRepository clientRepository;
    private final ViewClientRepository viewClientRepository;
    private final ContactInfoRepository contactInfoRepository;
    private final EncryptionService encryptionService;

    /**
     * Fallback key from config — used ONLY for:
     *  1. Admin views (admin has no per-user encryption key for other users)
     *  2. Sign-up (no JWT yet, encryption key provided externally)
     *  3. Backward compatibility with pre-migration data
     */
    @Value("${encryption.key}")
    private String fallbackEncryptionKey;

    public ClientService(ClientRepository clientRepository,
                         ViewClientRepository viewClientRepository,
                         ContactInfoRepository contactInfoRepository,
                         EncryptionService encryptionService) {
        this.clientRepository = clientRepository;
        this.viewClientRepository = viewClientRepository;
        this.contactInfoRepository = contactInfoRepository;
        this.encryptionService = encryptionService;
    }

    // --1 Create client (sign-up — no JWT yet, uses provided key or fallback)
    @Transactional
    public ClientDTO createClient(ClientDTO dto, String encryptionKey) throws Exception {
        String key = resolveKey(encryptionKey);

        boolean exists = clientRepository
                .findByLastNameContainingIgnoreCase(dto.getLastName())
                .stream()
                .anyMatch(c -> c.getFirstName().equalsIgnoreCase(dto.getFirstName()));

        if (exists) {
            throw new IllegalArgumentException(
                    "Client already exists: " + dto.getFirstName() + " " + dto.getLastName());
        }

        ClientType clientType = ClientType.fromCode(dto.getClientTypeCode());
        SexType sexType = SexType.fromCode(dto.getSexCode());

        Client entity = ClientMapper.toEntity(dto, clientType, sexType);

        entity.setFirstName(encryptionService.encrypt(entity.getFirstName(), key));
        entity.setLastName(encryptionService.encrypt(entity.getLastName(), key));

        final Client saved = clientRepository.save(entity);
        return ClientMapper.toDTO(saved, encryptionService, key, fallbackEncryptionKey);
    }

    // Backward-compatible overload for sign-up (no encryption key yet)
    @Transactional
    public ClientDTO createClient(ClientDTO dto) throws Exception {
        return createClient(dto, fallbackEncryptionKey);
    }

    // --2 Find clients by name (uses per-user key)
    @Transactional
    public List<ClientDTO> searchByName(String name, String encryptionKey) {
        String key = resolveKey(encryptionKey);
        return clientRepository
                .findByLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCase(name, name)
                .stream()
                .map(c -> ClientMapper.toDTO(c, encryptionService, key, fallbackEncryptionKey))
                .collect(Collectors.toList());
    }

    // --3 Update contact info (uses per-user key)
    @Transactional
    public ContactInfoDTO updateClientContactInfo(@NotNull Long clientId, ContactInfoDTO dto, String encryptionKey) {
        String key = resolveKey(encryptionKey);
        if (clientId == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }
        final Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with ID " + clientId));

        ContactInfo contactInfo = Optional.ofNullable(contactInfoRepository.findByClientId(clientId))
                .map(existing -> ContactInfoMapper.updateEntity(existing, dto, encryptionService, key))
                .orElseGet(() -> ContactInfoMapper.toEntity(dto, client, encryptionService, key));

        final ContactInfo saved = contactInfoRepository.save(contactInfo);
        return ContactInfoMapper.toDTO(saved, encryptionService, key, fallbackEncryptionKey);
    }

    // --4 Get client summary (uses per-user key)
    public Map<String, Object> getClientSummary(@NotNull Long clientId, String encryptionKey) {
        String key = resolveKey(encryptionKey);
        if (clientId == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }
        final Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        ContactInfoDTO contactInfo = ContactInfoMapper.toDTO(
                contactInfoRepository.findByClientId(clientId), encryptionService, key, fallbackEncryptionKey);
        Map<String, Object> summary = new HashMap<>();
        summary.put("client", ClientMapper.toDTO(client, encryptionService, key, fallbackEncryptionKey));
        summary.put("contactInfo", contactInfo);
        return summary;
    }

    // --5 Soft delete
    @Transactional
    public void deleteClient(@NotNull Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        if (!client.isActive()) {
            throw new ResourceNotFoundException("Client already inactive");
        }
        client.setActive(false);
        clientRepository.save(client);
    }

    /**
     * Admin suspend — idempotent soft-disable (same as delete but allows already-inactive client).
     */
    @Transactional
    public void suspendClient(@NotNull Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        if (client.isActive()) {
            client.setActive(false);
            clientRepository.save(client);
        }
    }

    /**
     * Admin list: names decrypted when possible; contact PII never decrypted — masked.
     * Admin uses fallback key since they don't have individual user keys.
     */
    public List<ViewClientDTO> getAllViewClients() {
        return viewClientRepository.findAll().stream().map(this::toAdminListViewDto).toList();
    }

    /** USER: full decrypted row for own client only (caller enforces via JWT). */
    public ViewClientDTO getViewClientForSelf(Long clientId, String encryptionKey) {
        String key = resolveKey(encryptionKey);
        ViewClient v = viewClientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found in view"));
        return toOwnerViewDto(v, key);
    }

    // ======================== RE-ENCRYPTION (internal, called from auth-service) ========================

    /**
     * Re-encrypts all personal data for a client when their password changes.
     * Called internally from auth-service via /api/internal/clients/re-encrypt.
     */
    @Transactional
    public void reEncryptClientData(Long clientId, String oldKey, String newKey) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found for re-encryption"));

        // Re-encrypt client names
        try {
            String decryptedFirst = safeDecrypt(client.getFirstName(), oldKey);
            String decryptedLast = safeDecrypt(client.getLastName(), oldKey);
            client.setFirstName(encryptionService.encrypt(decryptedFirst, newKey));
            client.setLastName(encryptionService.encrypt(decryptedLast, newKey));
            clientRepository.save(client);
        } catch (Exception e) {
            throw new RuntimeException("Failed to re-encrypt client names for clientId=" + clientId, e);
        }

        // Re-encrypt contact info
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

    /**
     * One-time migration: rows still encrypted with the legacy server key are re-encrypted with the user-derived key.
     */
    @Transactional
    public void migrateLegacyEncryption(Long clientId, String newKey) {
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
            encryptionService.decrypt(client.getFirstName(), fallbackEncryptionKey);
        } catch (Exception e) {
            return;
        }
        reEncryptClientData(clientId, fallbackEncryptionKey, newKey);
    }

    private String reEncryptField(String encryptedValue, String oldKey, String newKey) throws Exception {
        if (encryptedValue == null || encryptedValue.isBlank()) return encryptedValue;
        String decrypted = safeDecrypt(encryptedValue, oldKey);
        return encryptionService.encrypt(decrypted, newKey);
    }

    private String safeDecrypt(String encryptedValue, String key) {
        if (encryptedValue == null || encryptedValue.isBlank()) return encryptedValue;
        try {
            return encryptionService.decryptFlexible(encryptedValue, key, fallbackEncryptionKey);
        } catch (Exception e) {
            return encryptedValue; // Fallback to plaintext if not encrypted (e.g. seed data)
        }
    }

    // ======================== PRIVATE HELPERS ========================

    private String resolveKey(String encryptionKey) {
        return (encryptionKey != null && !encryptionKey.isBlank()) ? encryptionKey : fallbackEncryptionKey;
    }

    private ViewClientDTO toAdminListViewDto(ViewClient v) {
        ViewClientDTO dto = baseViewFields(v);
        dto.setEmail(PII_MASK);
        dto.setPhone(PII_MASK);
        dto.setAddress(PII_MASK);
        dto.setCity(PII_MASK);
        dto.setPostalCode(PII_MASK);
        try {
            dto.setFirstName(encryptionService.decrypt(v.getClientFirstName(), fallbackEncryptionKey));
            dto.setLastName(encryptionService.decrypt(v.getClientLastName(), fallbackEncryptionKey));
        } catch (Exception e) {
            dto.setFirstName(v.getClientFirstName());
            dto.setLastName(v.getClientLastName());
        }
        return dto;
    }

    private ViewClientDTO toOwnerViewDto(ViewClient v, String encryptionKey) {
        ViewClientDTO dto = baseViewFields(v);
        try {
            dto.setFirstName(encryptionService.decryptFlexible(v.getClientFirstName(), encryptionKey, fallbackEncryptionKey));
            dto.setLastName(encryptionService.decryptFlexible(v.getClientLastName(), encryptionKey, fallbackEncryptionKey));
            dto.setEmail(encryptionService.decryptFlexible(v.getEmailEncrypted(), encryptionKey, fallbackEncryptionKey));
            dto.setPhone(encryptionService.decryptFlexible(v.getPhoneEncrypted(), encryptionKey, fallbackEncryptionKey));
            dto.setAddress(encryptionService.decryptFlexible(v.getAddressEncrypted(), encryptionKey, fallbackEncryptionKey));
            dto.setCity(encryptionService.decryptFlexible(v.getCityEncrypted(), encryptionKey, fallbackEncryptionKey));
            dto.setPostalCode(encryptionService.decryptFlexible(v.getPostalCodeEncrypted(), encryptionKey, fallbackEncryptionKey));
        } catch (Exception e) {
            dto.setFirstName(v.getClientFirstName());
            dto.setLastName(v.getClientLastName());
            dto.setEmail(v.getEmailEncrypted());
            dto.setPhone(v.getPhoneEncrypted());
            dto.setAddress(v.getAddressEncrypted());
            dto.setCity(v.getCityEncrypted());
            dto.setPostalCode(v.getPostalCodeEncrypted());
        }
        return dto;
    }

    private static ViewClientDTO baseViewFields(ViewClient v) {
        ViewClientDTO dto = new ViewClientDTO();
        dto.setClientId(v.getClientId());
        dto.setClientType(v.getClientTypeName());
        dto.setRiskLevel(v.getRiskLevel());
        dto.setActive(v.getActive());
        dto.setCreatedAt(v.getCreatedAt());
        return dto;
    }
}