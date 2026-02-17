package ro.app.banking.dto.mapper;

import java.util.ArrayList;
import java.util.stream.Collectors;

import ro.app.banking.dto.ClientDTO;
import ro.app.banking.model.entity.Account;
import ro.app.banking.model.entity.Client;
import ro.app.banking.model.enums.ClientType;
import ro.app.banking.model.enums.SexType;

public class ClientMapper {
    public static ClientDTO toDTO(Client e) {
        ClientDTO dto = new ClientDTO();
        dto.setId(e.getId());
        dto.setLastName(e.getLastName());
        dto.setFirstName(e.getFirstName());
        dto.setClientTypeCode(e.getClientType() != null ? e.getClientType().getCode() : null);
        dto.setSexCode(e.getSexType() != null ? e.getSexType().getCode() : null);
        dto.setActive(e.isActive());
        dto.setAccountIds(e.getAccounts() != null
                ? e.getAccounts().stream().map(Account::getId).collect(Collectors.toList())
                : new ArrayList<>());
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
