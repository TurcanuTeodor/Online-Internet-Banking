package ro.app.backend_Java_SpringBoot.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "tranzactie")
public class TransactionTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "suma", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "suma_originala", nullable = false, precision = 15, scale = 2)
    private BigDecimal originalAmount;
    
    @Column(name = "semn", nullable = false)
    private String sign;

    @Column(name = "explicatii", columnDefinition = "TEXT")
    private String details;

    @Column(name = "data_tranzactie", nullable = false)
    private LocalDateTime transactionDate;

    @ManyToOne
    @JoinColumn(name = "cont_id")
    @JsonBackReference("account-transactions")
    private AccountTable account;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tip_tranzactie_id", nullable = false)
    private TransactionType transactionType;

    @ManyToOne(optional = true)
    @JoinColumn(name = "valuta_originala")
    private CurrencyType originalCurrency;

    public TransactionTable() {
    }

    public TransactionTable(AccountTable account, TransactionType transactionType, BigDecimal amount, BigDecimal originalAmount, CurrencyType originalCurrency, String sign, String details, LocalDateTime transactionDate) {
        this.account = account;
        this.transactionType = transactionType;
        this.amount = amount;
        this.originalAmount = originalAmount;
        this.originalCurrency = originalCurrency;
        this.sign = sign;
        this.details = details;
        this.transactionDate = transactionDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AccountTable getAccount() {
        return account;
    }

    public void setAccount(AccountTable account) {
        this.account = account;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public CurrencyType getOriginalCurrency() {
        return originalCurrency;
    }

    public void setOriginalCurrency(CurrencyType originalCurrency) {
        this.originalCurrency = originalCurrency;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    //hook for setting transaction date
    @PrePersist
    protected void onCreate() {
        if (this.transactionDate == null) {
            this.transactionDate = LocalDateTime.now();
        }
    }

}
