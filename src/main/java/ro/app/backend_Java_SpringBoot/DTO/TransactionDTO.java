package ro.app.backend_Java_SpringBoot.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionDTO {
    private Long id;

    @NotNull
    private Long accountId;

    @NotNull
    private Long transactionTypeId;

    @NotNull @Digits(integer=15, fraction=2)
    private BigDecimal amount;

    @NotNull @Digits(integer=15, fraction=2)
    private BigDecimal originalAmount;

    @NotNull
    private Long originalCurrencyId;

    @NotBlank
    private String sign; 

    @Size(max = 255)
    private String details;

    private LocalDateTime transactionDate;

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Long getTransactionTypeId() { return transactionTypeId; }
    public void setTransactionTypeId(Long transactionTypeId) { this.transactionTypeId = transactionTypeId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }
    public Long getOriginalCurrencyId() { return originalCurrencyId; }
    public void setOriginalCurrencyId(Long originalCurrencyId) { this.originalCurrencyId = originalCurrencyId; }
    public String getSign() { return sign; }
    public void setSign(String sign) { this.sign = sign; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
}
