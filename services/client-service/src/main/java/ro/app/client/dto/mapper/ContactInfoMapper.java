package ro.app.client.dto.mapper;

import ro.app.client.dto.ContactInfoDTO;
import ro.app.client.model.entity.ContactInfo;
import ro.app.client.model.entity.Client;
import ro.app.client.service.EncryptionService;

public class ContactInfoMapper {

    public static ContactInfoDTO toDTO(ContactInfo e, EncryptionService encryptionService, String key, String legacyKey) {
        if (e == null) return null;
        ContactInfoDTO dto = new ContactInfoDTO();
        try {
            dto.setPhone(encryptionService.decryptFlexible(e.getPhone(), key, legacyKey));
            dto.setEmail(encryptionService.decryptFlexible(e.getEmail(), key, legacyKey));
            dto.setContactPerson(encryptionService.decryptFlexible(e.getContactPerson(), key, legacyKey));
            dto.setWebsite(encryptionService.decryptFlexible(e.getWebsite(), key, legacyKey));
            dto.setAddress(encryptionService.decryptFlexible(e.getAddress(), key, legacyKey));
            dto.setCity(encryptionService.decryptFlexible(e.getCity(), key, legacyKey));
            dto.setPostalCode(encryptionService.decryptFlexible(e.getPostalCode(), key, legacyKey));
        } catch (Exception ex) {
            // fallback: return encrypted values if decryption fails
            dto.setPhone(e.getPhone());
            dto.setEmail(e.getEmail());
            dto.setContactPerson(e.getContactPerson());
            dto.setWebsite(e.getWebsite());
            dto.setAddress(e.getAddress());
            dto.setCity(e.getCity());
            dto.setPostalCode(e.getPostalCode());
        }
        return dto;
    }

    public static ContactInfo toEntity(ContactInfoDTO dto, Client client, EncryptionService encryptionService, String key) {
        ContactInfo e = new ContactInfo();
        try {
            e.setPhone(encryptionService.encrypt(dto.getPhone(), key));
            e.setEmail(encryptionService.encrypt(dto.getEmail(), key));
            e.setContactPerson(encryptionService.encrypt(dto.getContactPerson(), key));
            e.setWebsite(encryptionService.encrypt(dto.getWebsite(), key));
            e.setAddress(encryptionService.encrypt(dto.getAddress(), key));
            e.setCity(encryptionService.encrypt(dto.getCity(), key));
            e.setPostalCode(encryptionService.encrypt(dto.getPostalCode(), key));
        } catch (Exception ex) {
            // fallback: store plain values if encryption fails
            e.setPhone(dto.getPhone());
            e.setEmail(dto.getEmail());
            e.setContactPerson(dto.getContactPerson());
            e.setWebsite(dto.getWebsite());
            e.setAddress(dto.getAddress());
            e.setCity(dto.getCity());
            e.setPostalCode(dto.getPostalCode());
        }
        e.setClient(client);
        return e;
    }

    public static ContactInfo updateEntity(ContactInfo e, ContactInfoDTO dto, EncryptionService encryptionService, String key) {
        try {
            e.setPhone(encryptionService.encrypt(dto.getPhone(), key));
            e.setEmail(encryptionService.encrypt(dto.getEmail(), key));
            e.setContactPerson(encryptionService.encrypt(dto.getContactPerson(), key));
            e.setWebsite(encryptionService.encrypt(dto.getWebsite(), key));
            e.setAddress(encryptionService.encrypt(dto.getAddress(), key));
            e.setCity(encryptionService.encrypt(dto.getCity(), key));
            e.setPostalCode(encryptionService.encrypt(dto.getPostalCode(), key));
        } catch (Exception ex) {
            // fallback: store plain values if encryption fails
            e.setPhone(dto.getPhone());
            e.setEmail(dto.getEmail());
            e.setContactPerson(dto.getContactPerson());
            e.setWebsite(dto.getWebsite());
            e.setAddress(dto.getAddress());
            e.setCity(dto.getCity());
            e.setPostalCode(dto.getPostalCode());
        }
        return e;
    }
}
