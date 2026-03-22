package ro.app.payment.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;

import ro.app.payment.service.payment.PaymentService;

@RestController
@RequestMapping("/api/payments")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final PaymentService paymentService;

    @Value("${app.stripe.webhook-secret}")
    private String webhookSecret;

    public WebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            // Verify the webhook signature to ensure it comes from Stripe
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        String eventType = event.getType();
        log.info("Webhook received: {}", eventType);

        // Extract PaymentIntent ID from the event data
        event.getDataObjectDeserializer().getObject().ifPresent(stripeObject -> {
            if (stripeObject instanceof PaymentIntent intent) {
                paymentService.handleWebhookEvent(eventType, intent.getId());
            }
        });

        // Stripe expects 2xx response
        return ResponseEntity.ok("OK");
    }
}