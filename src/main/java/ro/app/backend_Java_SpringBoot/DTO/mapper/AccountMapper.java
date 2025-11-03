package ro.app.backend_Java_SpringBoot.DTO.mapper;

import ro.app.backend_Java_SpringBoot.DTO.AccountDTO;
import ro.app.backend_Java_SpringBoot.model.AccountTable;
import ro.app.backend_Java_SpringBoot.model.ClientTable;
import ro.app.backend_Java_SpringBoot.model.CurrencyType;
import ro.app.backend_Java_SpringBoot.model.TransactionTable;
import java.util.stream.Collectors;
import java.util.ArrayList;

public class AccountMapper {
    public static AccountDTO toDTO(AccountTable e) {
        AccountDTO dto = new AccountDTO();
        dto.setId(e.getId());
        dto.setIban(e.getIban());
        dto.setBalance(e.getBalance());
        dto.setCurrencyId(e.getCurrency() != null ? e.getCurrency().getId() : null);
        dto.setClientId(e.getClient() != null ? e.getClient().getId() : null);
        dto.setStatus(e.getStatus());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        dto.setTransactionIds(e.getTransactions() != null ?
                e.getTransactions().stream().map(TransactionTable::getId).collect(Collectors.toList())
                : new ArrayList<>());
        return dto;
    }

    public static AccountTable toEntity(AccountDTO dto, ClientTable client, CurrencyType currency) {
        AccountTable e = new AccountTable();
        e.setId(dto.getId());
        e.setIban(dto.getIban());
        e.setBalance(dto.getBalance());
        e.setClient(client);
        e.setCurrency(currency);
        if (dto.getStatus() != null) e.setStatus(dto.getStatus());
        return e;
    }
}
