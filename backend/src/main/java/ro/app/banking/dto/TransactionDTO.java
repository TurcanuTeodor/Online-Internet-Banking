package ro.app.banking.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionDTO {
    private Long id;

    @NotNull
    private Long accountId;

    @NotBlank
    private String transactionTypeCode;

    @NotNull @Digits(integer=15, fraction=2)
    private BigDecimal amount;

    @NotNull @Digits(integer=15, fraction=2)
    private BigDecimal originalAmount;

    private String originalCurrencyCode;

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
    public String getTransactionTypeCode() { return transactionTypeCode; }
    public void setTransactionTypeCode(String transactionTypeCode) { this.transactionTypeCode = transactionTypeCode; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }
    public String getOriginalCurrencyCode() { return originalCurrencyCode; }
    public void setOriginalCurrencyCode(String originalCurrencyCode) { this.originalCurrencyCode = originalCurrencyCode; }
    public String getSign() { return sign; }
    public void setSign(String sign) { this.sign = sign; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
}
