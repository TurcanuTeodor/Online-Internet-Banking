package ro.app.account.dto.mapper;

import java.math.BigDecimal;

import ro.app.account.dto.AccountDTO;
import ro.app.account.model.entity.Account;
import ro.app.account.model.enums.AccountStatus;
import ro.app.account.model.enums.CurrencyType;

public class AccountMapper {

    public static AccountDTO toDTO(Account e) {
        AccountDTO dto = new AccountDTO();
        dto.setId(e.getId());
        dto.setIban(e.getIban());
        dto.setBalance(e.getBalance());
        dto.setCurrencyCode(e.getCurrency() != null ? e.getCurrency().getCode() : null);
        dto.setClientId(e.getClientId()); // Distributed: direct Long, no Client entity
        dto.setStatus(e.getStatus() != null ? e.getStatus().name() : "ACTIVE");
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        return dto;
    }

    public static Account toEntity(AccountDTO dto) {
        Account e = new Account();
        e.setId(dto.getId());
        e.setIban(dto.getIban());
        e.setBalance(dto.getBalance() != null ? dto.getBalance() : BigDecimal.ZERO);
        e.setClientId(dto.getClientId());
        e.setCurrency(dto.getCurrencyCode() != null ? CurrencyType.fromCode(dto.getCurrencyCode()) : null);
        e.setStatus(dto.getStatus() != null ? AccountStatus.valueOf(dto.getStatus().toUpperCase()) : AccountStatus.ACTIVE);
        return e;
    }
}
