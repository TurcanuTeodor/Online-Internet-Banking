package ro.app.account.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class TransferRequest {

    @NotBlank(message = "Source IBAN is required")
    @Pattern(regexp = "^[A-Z]{2}\\d{2}[A-Z0-9]{1,30}$", message = "Invalid IBAN format")
    private String fromIban;

    @NotBlank(message = "Destination IBAN is required")
    @Pattern(regexp = "^[A-Z]{2}\\d{2}[A-Z0-9]{1,30}$", message = "Invalid IBAN format")
    private String toIban;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    public String getFromIban() { return fromIban; }
    public void setFromIban(String fromIban) { this.fromIban = fromIban; }

    public String getToIban() { return toIban; }
    public void setToIban(String toIban) { this.toIban = toIban; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
