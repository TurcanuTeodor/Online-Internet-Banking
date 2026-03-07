package ro.app.transaction.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import ro.app.transaction.model.enums.CurrencyType;
import ro.app.transaction.model.enums.TransactionCategory;
import ro.app.transaction.model.enums.TransactionType;

@Entity
@Table(name = "\"TRANSACTION\"")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // No @ManyToOne — cross-schema; store ID only
    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "destination_account_id")
    private Long destinationAccountId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "transaction_type", nullable = false, columnDefinition = "TRANSACTION_TYPE_ENUM")
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "category", nullable = false, columnDefinition = "TRANSACTION_CATEGORY_ENUM")
    private TransactionCategory category = TransactionCategory.OTHERS;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "original_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal originalAmount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "original_currency", columnDefinition = "CURRENCY_ENUM")
    private CurrencyType originalCurrency;

    @Column(name = "sign", nullable = false)
    private String sign;

    @Column(name = "merchant", length = 255)
    private String merchant;

    @Column(name = "details", length = 255)
    private String details;

    @Column(name = "risk_score", precision = 5, scale = 4)
    private BigDecimal riskScore;

    @Column(name = "flagged", nullable = false)
    private Boolean flagged = false;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Transaction() {
    }

    public Transaction(Long accountId, TransactionType transactionType, BigDecimal amount,
                       BigDecimal originalAmount, CurrencyType originalCurrency, String sign,
                       String details, LocalDateTime transactionDate) {
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.originalAmount = originalAmount;
        this.originalCurrency = originalCurrency;
        this.sign = sign;
        this.details = details;
        this.transactionDate = transactionDate;
        this.category = TransactionCategory.OTHERS;
        this.flagged = false;
    }

    @PrePersist
    protected void onCreate() {
        if (this.transactionDate == null) {
            this.transactionDate = LocalDateTime.now();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // ==================== Getters & Setters ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getDestinationAccountId() { return destinationAccountId; }
    public void setDestinationAccountId(Long destinationAccountId) { this.destinationAccountId = destinationAccountId; }

    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }

    public TransactionCategory getCategory() { return category; }
    public void setCategory(TransactionCategory category) { this.category = category != null ? category : TransactionCategory.OTHERS; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }

    public CurrencyType getOriginalCurrency() { return originalCurrency; }
    public void setOriginalCurrency(CurrencyType originalCurrency) { this.originalCurrency = originalCurrency; }

    public String getSign() { return sign; }
    public void setSign(String sign) { this.sign = sign; }

    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public BigDecimal getRiskScore() { return riskScore; }
    public void setRiskScore(BigDecimal riskScore) { this.riskScore = riskScore; }

    public Boolean getFlagged() { return flagged; }
    public void setFlagged(Boolean flagged) { this.flagged = flagged != null ? flagged : false; }

    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
