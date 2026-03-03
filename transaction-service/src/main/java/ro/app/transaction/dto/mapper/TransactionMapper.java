package ro.app.transaction.dto.mapper;

import ro.app.transaction.dto.TransactionDTO;
import ro.app.transaction.model.entity.Transaction;
import ro.app.transaction.model.enums.CurrencyType;
import ro.app.transaction.model.enums.TransactionCategory;
import ro.app.transaction.model.enums.TransactionType;

public class TransactionMapper {

    private TransactionMapper() {
        // Utility class
    }

    public static TransactionDTO toDTO(Transaction e) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(e.getId());
        dto.setAccountId(e.getAccountId());
        dto.setDestinationAccountId(e.getDestinationAccountId());
        dto.setTransactionTypeCode(e.getTransactionType() != null ? e.getTransactionType().getCode() : null);
        dto.setTransactionTypeName(e.getTransactionType() != null ? e.getTransactionType().getLabel() : null);
        dto.setCategoryCode(e.getCategory() != null ? e.getCategory().getCode() : null);
        dto.setAmount(e.getAmount());
        dto.setOriginalAmount(e.getOriginalAmount());
        dto.setOriginalCurrencyCode(e.getOriginalCurrency() != null ? e.getOriginalCurrency().getCode() : null);
        dto.setSign(e.getSign());
        dto.setMerchant(e.getMerchant());
        dto.setDetails(e.getDetails());
        dto.setRiskScore(e.getRiskScore());
        dto.setFlagged(e.getFlagged());
        dto.setTransactionDate(e.getTransactionDate());
        return dto;
    }

    public static Transaction toEntity(TransactionDTO dto) {
        Transaction e = new Transaction();
        e.setId(dto.getId());
        e.setAccountId(dto.getAccountId());
        e.setDestinationAccountId(dto.getDestinationAccountId());
        e.setTransactionType(TransactionType.fromCode(dto.getTransactionTypeCode()));
        e.setCategory(TransactionCategory.fromCode(dto.getCategoryCode()));
        e.setAmount(dto.getAmount());
        e.setOriginalAmount(dto.getOriginalAmount());
        e.setOriginalCurrency(dto.getOriginalCurrencyCode() != null
                ? CurrencyType.fromCode(dto.getOriginalCurrencyCode()) : null);
        e.setSign(dto.getSign());
        e.setMerchant(dto.getMerchant());
        e.setDetails(dto.getDetails());
        e.setRiskScore(dto.getRiskScore());
        e.setFlagged(dto.getFlagged());
        e.setTransactionDate(dto.getTransactionDate());
        return e;
    }
}
