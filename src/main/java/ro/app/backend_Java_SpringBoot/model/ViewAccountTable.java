package ro.app.backend_Java_SpringBoot.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "view_cont")
public class ViewAccountTable {
    @Id
    @Column(name = "cont_id")
    private Long accountId;

    @Column(name = "cont_iban")
    private String accountIban;

    @Column(name = "cont_sold")
    private BigDecimal accountBalance;

    @Column(name = "valuta_cod")
    private String currencyCode;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "client_nume")
    private String clientLastName;

    @Column(name = "client_prenume")
    private String clientFirstName;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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
