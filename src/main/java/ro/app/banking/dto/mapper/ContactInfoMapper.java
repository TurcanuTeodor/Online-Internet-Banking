package ro.app.banking.dto.mapper;

import ro.app.banking.dto.ContactInfoDTO;
import ro.app.banking.model.Client;
import ro.app.banking.model.ContactInfo;

public class ContactInfoMapper {

    public static ContactInfoDTO toDTO(ContactInfo e) {
        if (e == null) return null;
        ContactInfoDTO dto = new ContactInfoDTO();
        dto.setPhone(e.getPhone());
        dto.setEmail(e.getEmail());
        dto.setContactPerson(e.getContactPerson());
        dto.setWebsite(e.getWebsite());
        return dto;
    }

    public static ContactInfo toEntity(ContactInfoDTO dto, Client client) {
        ContactInfo e = new ContactInfo();
        e.setPhone(dto.getPhone());
        e.setEmail(dto.getEmail());
        e.setContactPerson(dto.getContactPerson());
        e.setWebsite(dto.getWebsite());
        e.setClient(client);
        return e;
    }

    public static ContactInfo updateEntity(ContactInfo e, ContactInfoDTO dto) {
        e.setPhone(dto.getPhone());
        e.setEmail(dto.getEmail());
        e.setContactPerson(dto.getContactPerson());
        e.setWebsite(dto.getWebsite());
        return e;
    }

}
