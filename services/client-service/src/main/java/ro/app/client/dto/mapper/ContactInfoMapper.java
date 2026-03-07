package ro.app.client.dto.mapper;

import ro.app.client.dto.ContactInfoDTO;
import ro.app.client.model.embedded.ContactInfo;
import ro.app.client.model.entity.Client;

public class ContactInfoMapper {

    public static ContactInfoDTO toDTO(ContactInfo e) {
        if (e == null) return null;
        ContactInfoDTO dto = new ContactInfoDTO();
        dto.setPhone(e.getPhone());
        dto.setEmail(e.getEmail());
        dto.setContactPerson(e.getContactPerson());
        dto.setWebsite(e.getWebsite());
        dto.setAddress(e.getAddress());
        dto.setCity(e.getCity());
        dto.setPostalCode(e.getPostalCode());
        return dto;
    }

    public static ContactInfo toEntity(ContactInfoDTO dto, Client client) {
        ContactInfo e = new ContactInfo();
        e.setPhone(dto.getPhone());
        e.setEmail(dto.getEmail());
        e.setContactPerson(dto.getContactPerson());
        e.setWebsite(dto.getWebsite());
        e.setAddress(dto.getAddress());
        e.setCity(dto.getCity());
        e.setPostalCode(dto.getPostalCode());
        e.setClient(client);
        return e;
    }

    public static ContactInfo updateEntity(ContactInfo e, ContactInfoDTO dto) {
        e.setPhone(dto.getPhone());
        e.setEmail(dto.getEmail());
        e.setContactPerson(dto.getContactPerson());
        e.setWebsite(dto.getWebsite());
        e.setAddress(dto.getAddress());
        e.setCity(dto.getCity());
        e.setPostalCode(dto.getPostalCode());
        return e;
    }
}
