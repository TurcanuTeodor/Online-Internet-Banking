package ro.app.client.service;

import java.util.List;

import org.springframework.stereotype.Service;

import ro.app.client.dto.ViewClientDTO;
import ro.app.client.exception.ResourceNotFoundException;
import ro.app.client.model.view.ViewClient;
import ro.app.client.repository.ViewClientRepository;

@Service
public class ClientViewProjectionService {

    private static final String PII_MASK = "[PROTECTED]";

    private final ViewClientRepository viewClientRepository;
    private final EncryptionService encryptionService;
    private final ClientKeyResolver keyResolver;

    public ClientViewProjectionService(
            ViewClientRepository viewClientRepository,
            EncryptionService encryptionService,
            ClientKeyResolver keyResolver) {
        this.viewClientRepository = viewClientRepository;
        this.encryptionService = encryptionService;
        this.keyResolver = keyResolver;
    }

    public List<ViewClientDTO> getAllViewClients() {
        return viewClientRepository.findAll().stream().map(this::toAdminListViewDto).toList();
    }

    public ViewClientDTO getViewClientForSelf(Long clientId, String encryptionKey) {
        if (clientId == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }
        String key = keyResolver.resolveKey(encryptionKey);
        ViewClient v = viewClientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found in view"));
        return toOwnerViewDto(v, key);
    }

    /**
     * Admin analytic view — returns decrypted PII using server fallback key.
     * Name is fully visible; email and phone are sent decrypted (masking done on frontend).
     * City and postalCode: new saves are plaintext; legacy encrypted values are decrypted on read.
     */
    private ViewClientDTO toAdminListViewDto(ViewClient v) {
        ViewClientDTO dto = baseViewFields(v);
        String key = keyResolver.fallbackKey();
        dto.setFirstName(safeDecrypt(v.getClientFirstName(), key));
        dto.setLastName(safeDecrypt(v.getClientLastName(), key));
        dto.setEmail(safeDecrypt(v.getEmailEncrypted(), key));
        dto.setPhone(safeDecrypt(v.getPhoneEncrypted(), key));
        dto.setCity(decryptOrPlain(v.getCityEncrypted(), key));
        dto.setPostalCode(decryptOrPlain(v.getPostalCodeEncrypted(), key));
        return dto;
    }

    /**
     * Safely decrypt a value using the server key, returning the raw value on any failure.
     * Returns null if the input is null or blank.
     */
    private String safeDecrypt(String value, String key) {
        if (value == null || value.isBlank()) return null;
        try {
            return encryptionService.decryptFlexible(value, key, null);
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * Tries to decrypt the value; if it is not in encrypted format (new plaintext saves),
     * returns the value as-is. Supports transition from encrypted to plaintext storage.
     */
    private String decryptOrPlain(String value, String key) {
        if (value == null || value.isBlank()) return value;
        // Encrypted format: "base64salt:base64iv:base64ciphertext" (exactly 2 colons)
        String[] parts = value.split(":");
        if (parts.length != 3) return value; // plaintext — return as-is
        try {
            return encryptionService.decryptFlexible(value, key, null);
        } catch (Exception e) {
            return value;
        }
    }

    private ViewClientDTO toOwnerViewDto(ViewClient v, String encryptionKey) {
        ViewClientDTO dto = baseViewFields(v);
        try {
            dto.setFirstName(
                    encryptionService.decryptFlexible(v.getClientFirstName(), encryptionKey, keyResolver.fallbackKey()));
            dto.setLastName(
                    encryptionService.decryptFlexible(v.getClientLastName(), encryptionKey, keyResolver.fallbackKey()));
            dto.setEmail(
                    encryptionService.decryptFlexible(v.getEmailEncrypted(), encryptionKey, keyResolver.fallbackKey()));
            dto.setPhone(
                    encryptionService.decryptFlexible(v.getPhoneEncrypted(), encryptionKey, keyResolver.fallbackKey()));
            dto.setAddress(
                    encryptionService.decryptFlexible(v.getAddressEncrypted(), encryptionKey, keyResolver.fallbackKey()));
            dto.setCity(
                    encryptionService.decryptFlexible(v.getCityEncrypted(), encryptionKey, keyResolver.fallbackKey()));
            dto.setPostalCode(
                    encryptionService.decryptFlexible(v.getPostalCodeEncrypted(), encryptionKey, keyResolver.fallbackKey()));
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