package ro.app.banking.model.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Immutable
@Table(name = "\"VIEW_TRANSACTION\"")
public class ViewTransaction {
    @Id
    @Column(name="transaction_id")
    private Long transactionId;

    @Column(name="source_iban")
    private String sourceIban;

    @Column(name="source_client_id")
    private Long sourceClientId;

    @Column(name="source_first_name")
    private String sourceFirstName;

    @Column(name="source_last_name")
    private String sourceLastName;

    @Column(name="dest_iban")
    private String destIban;

    @Column(name="dest_client_id")
    private Long destClientId;

    @Column(name="dest_first_name")
    private String destFirstName;

    @Column(name="dest_last_name")
    private String destLastName;

    @Column(name="transaction_type_name")
    private String transactionTypeName;

    @Column(name="transaction_amount")
    private BigDecimal transactionAmount;

    @Column(name="transaction_original_amount")
    private BigDecimal transactionOriginalAmount;

    @Column(name="transaction_sign")
    private String transactionSign;

    @Column(name="currency_type_code")
    private String currencyCode;

    @Column(name="transaction_date")
    private LocalDateTime transactionDate;

    @Column(name="transaction_details")
    private String details;

    @Column(name="fraud_score")
    private BigDecimal fraudScore;

    // Getters
    public Long getTransactionId() { return transactionId; }
    public String getSourceIban() { return sourceIban; }
    public Long getSourceClientId() { return sourceClientId; }
    public String getSourceFirstName() { return sourceFirstName; }
    public String getSourceLastName() { return sourceLastName; }
    public String getDestIban() { return destIban; }
    public Long getDestClientId() { return destClientId; }
    public String getDestFirstName() { return destFirstName; }
    public String getDestLastName() { return destLastName; }
    public String getTransactionTypeName() { return transactionTypeName; }
    public BigDecimal getTransactionAmount() { return transactionAmount; }
    public BigDecimal getTransactionOriginalAmount() { return transactionOriginalAmount; }
    public String getTransactionSign() { return transactionSign; }
    public String getCurrencyCode() { return currencyCode; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public String getDetails() { return details; }
    public BigDecimal getFraudScore() { return fraudScore; }
}
