package ro.app.banking.dto.mapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.stream.Collectors;

import ro.app.banking.dto.AccountDTO;
import ro.app.banking.model.Account;
import ro.app.banking.model.Client;
import ro.app.banking.model.CurrencyType;
import ro.app.banking.model.Transaction;

public class AccountMapper {

    public static AccountDTO toDTO(Account e) {
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
        e.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIV");
        return e;
    }
}
