package ro.app.payment.config;

import org.springframework.context.annotation.Configuration;

import com.stripe.Stripe;

import jakarta.annotation.PostConstruct;

@Configuration
public class StripeConfig {
    private final String secretKey;

    public StripeConfig(PaymentProperties paymentProperties) {
        this.secretKey = paymentProperties.getStripe().getApiKey();
    }

    @PostConstruct
    public void init(){
        Stripe.apiKey = secretKey;  //global static field from Stripe SDK
    }
}
