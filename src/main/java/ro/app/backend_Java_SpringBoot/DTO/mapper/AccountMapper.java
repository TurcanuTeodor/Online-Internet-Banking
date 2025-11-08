package ro.app.backend_Java_SpringBoot.dto.mapper;

import ro.app.backend_Java_SpringBoot.dto.AccountDTO;
import ro.app.backend_Java_SpringBoot.model.*;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.math.BigDecimal;

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
        dto.setTransactionIds(
                e.getTransactions() != null
                        ? e.getTransactions().stream().map(TransactionTable::getId).collect(Collectors.toList())
                        : new ArrayList<>());
        return dto;
    }

    public static AccountTable toEntity(AccountDTO dto, ClientTable client, CurrencyType currency) {
        AccountTable e = new AccountTable();
        e.setId(dto.getId());
        e.setIban(dto.getIban());
        e.setBalance(dto.getBalance() != null ? dto.getBalance() : BigDecimal.ZERO);
        e.setClient(client);
        e.setCurrency(currency);
        e.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIV");
        return e;
    }
}
