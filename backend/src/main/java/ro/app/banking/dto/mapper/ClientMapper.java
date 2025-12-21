package ro.app.banking.dto.mapper;

import java.util.ArrayList;
import java.util.stream.Collectors;

import ro.app.banking.dto.ClientDTO;
import ro.app.banking.model.Account;
import ro.app.banking.model.Client;
import ro.app.banking.model.ClientType;
import ro.app.banking.model.SexType;

public class ClientMapper {
    public static ClientDTO toDTO(Client e) {
        ClientDTO dto = new ClientDTO();
        dto.setId(e.getId());
        dto.setLastName(e.getLastName());
        dto.setFirstName(e.getFirstName());
        dto.setClientTypeId(e.getClientType() != null ? e.getClientType().getId() : null);
        dto.setSexId(e.getSex() != null ? e.getSex().getId() : null);
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
        e.setSex(st);
        e.setActive(dto.isActive());
        return e;
    }
       
}
