package ro.app.payment.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * One-time card top-up: currency is taken from the target account (EUR/RON).
 */
public class CreateTopUpIntentRequest {

    @NotNull
    private Long accountId;

    @NotNull
    @DecimalMin(value = "0.50", message = "Minimum top-up amount is 0.50")
    private BigDecimal amount;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
