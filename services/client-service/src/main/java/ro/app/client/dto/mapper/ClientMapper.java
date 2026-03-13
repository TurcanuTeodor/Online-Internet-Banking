package ro.app.client.dto.mapper;

import ro.app.client.dto.ClientDTO;
import ro.app.client.model.entity.Client;
import ro.app.client.model.enums.ClientType;
import ro.app.client.model.enums.SexType;
import ro.app.client.service.EncryptionService;

public class ClientMapper {

    public static ClientDTO toDTO(Client e, EncryptionService encryptionService, String key) {
        ClientDTO dto = new ClientDTO();
        dto.setId(e.getId());
        try {
            dto.setLastName(encryptionService.decrypt(e.getLastName(), key));
            dto.setFirstName(encryptionService.decrypt(e.getFirstName(), key));
        } catch (Exception ex) {
            // fallback: return encrypted values if decryption fails
            dto.setLastName(e.getLastName());
            dto.setFirstName(e.getFirstName());
        }
        dto.setClientTypeCode(e.getClientType() != null ? e.getClientType().getCode() : null);
        dto.setSexCode(e.getSexType() != null ? e.getSexType().getCode() : null);
        dto.setActive(e.isActive());
        return dto;
    }

    public static Client toEntity(ClientDTO dto, ClientType ct, SexType st) {
        Client e = new Client();
        e.setId(dto.getId());
        e.setLastName(dto.getLastName());
        e.setFirstName(dto.getFirstName());
        e.setClientType(ct);
        e.setSexType(st);
        e.setActive(dto.isActive());
        return e;
    }
}
