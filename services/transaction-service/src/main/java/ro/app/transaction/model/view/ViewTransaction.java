package ro.app.transaction.model.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Read-only view entity mapped to VIEW_TRANSACTION.
 * Simplified for microservice: no cross-schema JOINs to CLIENT.
 * Contains transaction data + account IDs only.
 */
@Entity
@Immutable
@Table(name = "\"VIEW_TRANSACTION\"")
public class ViewTransaction {

    @Id
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "destination_account_id")
    private Long destinationAccountId;

    @Column(name = "transaction_type")
    private String transactionType;

    @Column(name = "category")
    private String category;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "original_amount")
    private BigDecimal originalAmount;

    @Column(name = "original_currency")
    private String originalCurrency;

    @Column(name = "sign")
    private String sign;

    @Column(name = "merchant")
    private String merchant;

    @Column(name = "details")
    private String details;

    @Column(name = "risk_score")
    private BigDecimal riskScore;

    @Column(name = "flagged")
    private Boolean flagged;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    // ==================== Getters ====================

    public Long getTransactionId() { return transactionId; }
    public Long getAccountId() { return accountId; }
    public Long getDestinationAccountId() { return destinationAccountId; }
    public String getTransactionType() { return transactionType; }
    public String getCategory() { return category; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public String getOriginalCurrency() { return originalCurrency; }
    public String getSign() { return sign; }
    public String getMerchant() { return merchant; }
    public String getDetails() { return details; }
    public BigDecimal getRiskScore() { return riskScore; }
    public Boolean getFlagged() { return flagged; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
}
