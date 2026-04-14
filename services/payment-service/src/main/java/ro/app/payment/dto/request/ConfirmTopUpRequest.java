package ro.app.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

public class ConfirmTopUpRequest {

    @NotBlank(message = "stripePaymentIntentId is required")
    private String stripePaymentIntentId;

    public String getStripePaymentIntentId() { return stripePaymentIntentId; }
    public void setStripePaymentIntentId(String stripePaymentIntentId) { this.stripePaymentIntentId = stripePaymentIntentId; }
}

