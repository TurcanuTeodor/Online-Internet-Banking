package ro.app.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TransactionDTO {

    private Long id;

    @NotNull
    private Long accountId;

    private Long destinationAccountId;

    @NotBlank
    private String transactionTypeCode;

    private String transactionTypeName;

    private String categoryCode;

    @NotNull
    @Digits(integer = 15, fraction = 2)
    private BigDecimal amount;

    @NotNull
    @Digits(integer = 15, fraction = 2)
    private BigDecimal originalAmount;

    private String originalCurrencyCode;

    @NotBlank
    private String sign;

    @Size(max = 255)
    private String merchant;

    @Size(max = 255)
    private String details;

    private BigDecimal riskScore;

    private Boolean flagged;

    private LocalDateTime transactionDate;

    // ==================== Getters & Setters ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getDestinationAccountId() { return destinationAccountId; }
    public void setDestinationAccountId(Long destinationAccountId) { this.destinationAccountId = destinationAccountId; }

    public String getTransactionTypeCode() { return transactionTypeCode; }
    public void setTransactionTypeCode(String transactionTypeCode) { this.transactionTypeCode = transactionTypeCode; }

    public String getTransactionTypeName() { return transactionTypeName; }
    public void setTransactionTypeName(String transactionTypeName) { this.transactionTypeName = transactionTypeName; }

    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }

    public String getOriginalCurrencyCode() { return originalCurrencyCode; }
    public void setOriginalCurrencyCode(String originalCurrencyCode) { this.originalCurrencyCode = originalCurrencyCode; }

    public String getSign() { return sign; }
    public void setSign(String sign) { this.sign = sign; }

    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public BigDecimal getRiskScore() { return riskScore; }
    public void setRiskScore(BigDecimal riskScore) { this.riskScore = riskScore; }

    public Boolean getFlagged() { return flagged; }
    public void setFlagged(Boolean flagged) { this.flagged = flagged; }

    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
}
