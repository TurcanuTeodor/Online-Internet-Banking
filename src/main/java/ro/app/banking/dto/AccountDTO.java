package ro.app.banking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.constraints.*;

public class AccountDTO {

    private Long id;

    @NotBlank
    @Size(max = 34)
    private String iban;

    @NotNull
    @Digits(integer = 15, fraction = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @NotNull
    private Long currencyId;

    @NotNull
    private Long clientId;

    @NotBlank
    private String status = "ACTIVE";

    @NotNull
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<Long> transactionIds = new ArrayList<>();

    // Getters / Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public Long getCurrencyId() { return currencyId; }
    public void setCurrencyId(Long currencyId) { this.currencyId = currencyId; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<Long> getTransactionIds() { return transactionIds; }
    public void setTransactionIds(List<Long> transactionIds) { this.transactionIds = transactionIds; }
}
