package ro.app.payment.dto.response;

import java.math.BigDecimal;

/**
 * Returned to the SPA so Stripe.js can confirm the PaymentIntent with {@code CardElement}.
 */
public class TopUpIntentResponse {

    private String clientSecret;
    private Long paymentId;
    private String stripePaymentIntentId;
    private String currencyCode;
    private BigDecimal amount;

    public TopUpIntentResponse() {
    }

    public TopUpIntentResponse(
            String clientSecret,
            Long paymentId,
            String stripePaymentIntentId,
            String currencyCode,
            BigDecimal amount) {
        this.clientSecret = clientSecret;
        this.paymentId = paymentId;
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.currencyCode = currencyCode;
        this.amount = amount;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public void setStripePaymentIntentId(String stripePaymentIntentId) {
        this.stripePaymentIntentId = stripePaymentIntentId;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
