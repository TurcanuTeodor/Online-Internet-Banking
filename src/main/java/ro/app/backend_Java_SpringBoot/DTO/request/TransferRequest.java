package ro.app.backend_Java_SpringBoot.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class TransferRequest {
    @NotBlank
    private String fromIban;

    @NotBlank
    private String toIban;

    @NotNull
    @Digits(integer = 15, fraction = 2)
    private BigDecimal amount;

    public String getFromIban() { return fromIban; }
    public void setFromIban(String fromIban) { this.fromIban = fromIban; }

    public String getToIban() { return toIban; }
    public void setToIban(String toIban) { this.toIban = toIban; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
