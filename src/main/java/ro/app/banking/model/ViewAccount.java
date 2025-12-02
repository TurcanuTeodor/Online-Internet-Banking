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
@Table(name = "view_account")
public class ViewAccount {
    @Id
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "account_iban")
    private String accountIban;

    @Column(name = "account_balance")
    private BigDecimal accountBalance;

    @Column(name = "currency_type_code")
    private String currencyCode;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "client_last_name")
    private String clientLastName;

    @Column(name = "client_first_name")
    private String clientFirstName;

    @Column(name = "account_status")
    private String status;

    @Column(name = "account_created_at")
    private LocalDateTime createdAt;

    @Column(name = "account_updated_at")
    private LocalDateTime updatedAt;

    public Long getAccountId() { return accountId; }
    public String getAccountIban() { return accountIban; }
    public BigDecimal getAccountBalance() { return accountBalance; }
    public String getCurrencyCode() { return currencyCode; }
    public Long getClientId() { return clientId; }
    public String getClientLastName() { return clientLastName; }
    public String getClientFirstName() { return clientFirstName; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
