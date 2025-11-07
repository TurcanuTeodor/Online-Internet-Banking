package ro.app.backend_Java_SpringBoot.dto.mapper;

import ro.app.backend_Java_SpringBoot.dto.ContactInfoDTO;
import ro.app.backend_Java_SpringBoot.model.ClientTable;
import ro.app.backend_Java_SpringBoot.model.ContactInfo;

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

    public static ContactInfo toEntity(ContactInfoDTO dto, ClientTable client) {
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
