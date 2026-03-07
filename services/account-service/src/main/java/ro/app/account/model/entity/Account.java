package ro.app.account.model.entity;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import ro.app.account.model.enums.AccountStatus;
import ro.app.account.model.enums.CurrencyType;

@Entity
@Table(name = "\"ACCOUNT\"")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "iban", nullable = false, unique = true)
    private String iban;

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "currency_code", nullable = false, columnDefinition = "CURRENCY_ENUM")
    private CurrencyType currency;

    // Distributed: no @ManyToOne Client — just store the ID
    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "ACCOUNT_STATUS_ENUM")
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Distributed: no List<Transaction> — transaction-service owns transactions

    public Account() {
    }

    public Account(String iban, CurrencyType currency, Long clientId) {
        this.iban = iban;
        this.currency = currency;
        this.clientId = clientId;
        this.balance = BigDecimal.ZERO;
        this.status = AccountStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    public Account(String iban, BigDecimal balance, CurrencyType currency,
                   Long clientId, AccountStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.iban = iban;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
        this.currency = currency;
        this.clientId = clientId;
        this.status = (status != null) ? status : AccountStatus.ACTIVE;
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt;
    }

    // === GETTERS / SETTERS ===

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public CurrencyType getCurrency() { return currency; }
    public void setCurrency(CurrencyType currency) { this.currency = currency; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // === HOOKS ===
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.balance == null) this.balance = BigDecimal.ZERO;
        if (this.status == null) this.status = AccountStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
