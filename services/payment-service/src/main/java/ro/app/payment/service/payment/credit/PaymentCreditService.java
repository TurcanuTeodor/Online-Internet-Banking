package ro.app.payment.service.payment.credit;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import ro.app.payment.model.entity.Payment;

/**
 * Applies settled Stripe funds to the core ledger via account-service (which also records DEPOSIT in transaction-service).
 * 
 * Migrated from RestTemplate to RestClient (Spring Boot 3.2+) for fluent API and observation support.
 */
@Service
public class PaymentCreditService {

    private static final Logger log = LoggerFactory.getLogger(PaymentCreditService.class);

    private final RestClient restClient;

    @Value("${app.services.account.url}")
    private String accountServiceBaseUrl;

    @Value("${app.internal.api-secret}")
    private String internalApiSecret;

    public PaymentCreditService(RestClient restClient) {
        this.restClient = restClient;
    }

    public void applyCreditViaAccountService(Payment payment) {
        String url = accountServiceBaseUrl.replaceAll("/$", "") + "/api/internal/stripe-top-up/apply";

        Map<String, Object> body = new HashMap<>();
        body.put("accountId", payment.getAccountId());
        body.put("amount", payment.getAmount());
        body.put("currencyCode", payment.getCurrency().getCode());
        body.put("stripePaymentIntentId", payment.getStripePaymentIntentId());

        try {
            restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Internal-Api-Secret", internalApiSecret)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Failed to apply Stripe top-up credit for intent {}: {}", payment.getStripePaymentIntentId(), e.getMessage());
            throw new RuntimeException("Settlement failed: " + e.getMessage(), e);
        }
    }
}
