package ro.app.banking.dto.mapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.stream.Collectors;

import ro.app.banking.dto.AccountDTO;
import ro.app.banking.model.entity.Account;
import ro.app.banking.model.entity.Client;
import ro.app.banking.model.entity.Transaction;
import ro.app.banking.model.enums.AccountStatus;
import ro.app.banking.model.enums.CurrencyType;

public class AccountMapper {

    public static AccountDTO toDTO(Account e) {
        AccountDTO dto = new AccountDTO();
        dto.setId(e.getId());
        dto.setIban(e.getIban());
        dto.setBalance(e.getBalance());
        dto.setCurrencyCode(e.getCurrency() != null ? e.getCurrency().getCode() : null);
        dto.setClientId(e.getClient() != null ? e.getClient().getId() : null);
        dto.setStatus(e.getStatus() != null ? e.getStatus().name() : "ACTIVE");
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        dto.setTransactionIds(
                e.getTransactions() != null
                        ? e.getTransactions().stream().map(Transaction::getId).collect(Collectors.toList())
                        : new ArrayList<>());
        return dto;
    }

    public static Account toEntity(AccountDTO dto, Client client, CurrencyType currency) {
        Account e = new Account();
        e.setId(dto.getId());
        e.setIban(dto.getIban());
        e.setBalance(dto.getBalance() != null ? dto.getBalance() : BigDecimal.ZERO);
        e.setClient(client);
        e.setCurrency(currency);
        e.setStatus(dto.getStatus() != null ? AccountStatus.valueOf(dto.getStatus().toUpperCase()) : AccountStatus.ACTIVE);
        return e;
    }
}
