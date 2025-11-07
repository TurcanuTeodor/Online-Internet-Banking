package ro.app.backend_Java_SpringBoot.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "view_tranzactie")
public class ViewTransactionTable {
    @Id
    @Column(name="transaction_id")
    private Long transactionId;

    @Column(name="cont_iban")
    private String accountIban;

    @Column(name="tip_tranzactie_denumire")
    private String transactionTypeName;

    @Column(name="tranzactie_suma")
    private BigDecimal transactionAmount;

    @Column(name="tranzactie_semn")
    private String transactionSign;

    @Column(name="valuta_cod")
    private String currencyCode;

    @Column(name="tranzactie_data")
    private LocalDateTime transactionDate;

    @Column(name="explicatii")
    private String details;

    public Long getTransactionId() { return transactionId; }
    public String getAccountIban() { return accountIban; }
    public String getTransactionTypeName() { return transactionTypeName; }
    public BigDecimal getTransactionAmount() { return transactionAmount; }
    public String getTransactionSign() { return transactionSign; }
    public String getCurrencyCode() { return currencyCode; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public String getDetails(){ return details;}
}
