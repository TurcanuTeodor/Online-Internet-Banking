package ro.app.backend_Java_SpringBoot.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Entity
@Table(name = "cont")
public class AccountTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "iban", nullable = false, unique = true)
    private String iban;

    @Column(name = "sold", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @ManyToOne(optional = false)
    @JoinColumn(name = "valuta_id")
    private CurrencyType currency;

    @ManyToOne
    @JoinColumn(name = "client_id")
    @JsonBackReference("client-accounts")
    private ClientTable client;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    @JsonManagedReference("account-transactions")
    private List<TransactionTable> transactions = new ArrayList<>();

    @Column(name = "status", nullable = false)
    private String status = "ACTIV";

    @Column(name = "data_deschidere", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "data_actualizare")
    private LocalDateTime updatedAt;

    public AccountTable() {
    }

    //Constructor for new account with default balance and status
     public AccountTable(String iban, CurrencyType currency, ClientTable client) {
        this.iban = iban;
        this.currency = currency;
        this.client = client;
        this.balance = BigDecimal.ZERO;
        this.status = "ACTIV";
        this.createdAt = LocalDateTime.now();
    }

    public AccountTable(String iban, BigDecimal balance, CurrencyType currency,
                        ClientTable client, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.iban = iban;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
        this.currency = currency;
        this.client = client;
        this.status = (status != null && !status.isEmpty()) ? status : "ACTIV";
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public CurrencyType getCurrency() {
        return currency;
    }

    public void setCurrency(CurrencyType currency) {
        this.currency = currency;
    }

    public ClientTable getClient() {
        return client;
    }

    public void setClient(ClientTable client) {
        this.client = client;
    }

     public List<TransactionTable> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<TransactionTable> transactions) {
        this.transactions = transactions;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // === HOOKS pentru audit automat ===
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.balance == null) this.balance = BigDecimal.ZERO;
        if (this.status == null) this.status = "ACTIV";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // === METODE UTILE ===
    public void addTransaction(TransactionTable transaction) {
        transactions.add(transaction);
        transaction.setAccount(this);
    }

    public void removeTransaction(TransactionTable transaction) {
        transactions.remove(transaction);
        transaction.setAccount(null);
    }
}