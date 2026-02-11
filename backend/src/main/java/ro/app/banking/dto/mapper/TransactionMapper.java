package ro.app.banking.dto.mapper;

import ro.app.banking.dto.TransactionDTO;
import ro.app.banking.model.entity.Account;
import ro.app.banking.model.entity.Transaction;
import ro.app.banking.model.enums.CurrencyType;
import ro.app.banking.model.enums.TransactionType;

public class TransactionMapper {
    public static TransactionDTO toDTO(Transaction e) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(e.getId());
        dto.setAccountId(e.getAccount() != null ? e.getAccount().getId() : null);
        dto.setTransactionTypeCode(e.getTransactionType() != null ? e.getTransactionType().getCode() : null);
        dto.setAmount(e.getAmount());
        dto.setOriginalAmount(e.getOriginalAmount());
        dto.setOriginalCurrencyCode(e.getOriginalCurrency() != null ? e.getOriginalCurrency().getCode() : null);
        dto.setSign(e.getSign());
        dto.setDetails(e.getDetails());
        dto.setTransactionDate(e.getTransactionDate());
        return dto;
    }

    public static Transaction toEntity(TransactionDTO dto,
                                            Account account,
                                            TransactionType type,
                                            CurrencyType originalCurrency) {
        Transaction e = new Transaction();
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
