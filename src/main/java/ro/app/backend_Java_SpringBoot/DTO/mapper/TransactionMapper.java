package ro.app.backend_Java_SpringBoot.dto.mapper;

import ro.app.backend_Java_SpringBoot.dto.TransactionDTO;
import ro.app.backend_Java_SpringBoot.model.*;

public class TransactionMapper {
    public static TransactionDTO toDTO(TransactionTable e) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(e.getId());
        dto.setAccountId(e.getAccount() != null ? e.getAccount().getId() : null);
        dto.setTransactionTypeId(e.getTransactionType() != null ? e.getTransactionType().getId() : null);
        dto.setAmount(e.getAmount());
        dto.setOriginalAmount(e.getOriginalAmount());
        dto.setOriginalCurrencyId(e.getOriginalCurrency() != null ? e.getOriginalCurrency().getId() : null);
        dto.setSign(e.getSign());
        dto.setDetails(e.getDetails());
        dto.setTransactionDate(e.getTransactionDate());
        return dto;
    }

    public static TransactionTable toEntity(TransactionDTO dto,
                                            AccountTable account,
                                            TransactionType type,
                                            CurrencyType originalCurrency) {
        TransactionTable e = new TransactionTable();
        e.setId(dto.getId());
        e.setAccount(account);
        e.setTransactionType(type);
        e.setAmount(dto.getAmount());
        e.setOriginalAmount(dto.getOriginalAmount());
        e.setOriginalCurrency(originalCurrency);
        e.setSign(dto.getSign());
        e.setDetails(dto.getDetails());
        e.setTransactionDate(dto.getTransactionDate());
        return e;
    }
}
