package ro.app.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AttachPaymentMethodRequest {

    @NotNull
    private Long clientId;

    @NotBlank
    private String stripePaymentMethodId; // token from Stripe.js frontend

    //  Getters & Setters 

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getStripePaymentMethodId() { return stripePaymentMethodId; }
    public void setStripePaymentMethodId(String stripePaymentMethodId) { this.stripePaymentMethodId = stripePaymentMethodId; }
}