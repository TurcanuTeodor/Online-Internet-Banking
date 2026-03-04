package ro.app.payment.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreatePaymentRequest {

    @NotNull
    private Long clientId;

    @NotNull
    private Long accountId;

    @NotNull
    @DecimalMin(value = "0.50", message = "Minimum payment amount is 0.50")
    private BigDecimal amount;

    @NotBlank
    private String currencyCode;

    @NotBlank
    private String paymentMethodId; // Stripe PM ID, exemplu "pm_test_visa_4242"

    @Size(max = 500)
    private String description;

    //  Getters & Setters 

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public String getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(String paymentMethodId) { this.paymentMethodId = paymentMethodId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}