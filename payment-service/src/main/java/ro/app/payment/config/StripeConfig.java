package ro.app.payment.config;

import com.stripe.Stripe;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {
    @Value("${app.stripe.secret-key")
    private String secretKey;

    @PreConstruct
    public void init(){
        Stripe.apiKey= secretKey;  //gloabl static field from Stripe SDK
    }
}
