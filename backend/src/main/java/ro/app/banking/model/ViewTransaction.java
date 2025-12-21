package ro.app.banking.model;

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

    @Column(name="account_iban")
    private String accountIban;

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

    public Long getTransactionId() { return transactionId; }
    public String getAccountIban() { return accountIban; }
    public String getTransactionTypeName() { return transactionTypeName; }
    public BigDecimal getTransactionAmount() { return transactionAmount; }
    public String getTransactionSign() { return transactionSign; }
    public String getCurrencyCode() { return currencyCode; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public String getDetails(){ return details;}
    public BigDecimal getTransactionOriginalAmount() {return transactionOriginalAmount;}
}
