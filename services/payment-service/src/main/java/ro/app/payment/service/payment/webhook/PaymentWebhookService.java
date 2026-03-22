package ro.app.payment.service.payment.webhook;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ro.app.payment.model.entity.Payment;
import ro.app.payment.model.enums.PaymentStatus;
import ro.app.payment.repository.PaymentRepository;
import ro.app.payment.service.payment.credit.PaymentCreditService;

/**
 * Stripe webhook handling: event routing, idempotent settlement, status updates.
 */
@Service
public class PaymentWebhookService {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentCreditService paymentCreditService;

    public PaymentWebhookService(PaymentRepository paymentRepository, PaymentCreditService paymentCreditService) {
        this.paymentRepository = paymentRepository;
        this.paymentCreditService = paymentCreditService;
    }

    public void handleWebhookEvent(String eventType, String paymentIntentId) {
        Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElse(null);

        if (payment == null) {
            log.warn("Webhook: no payment found for intent {}", paymentIntentId);
            return;
        }

        switch (eventType) {
            case "payment_intent.succeeded" -> handlePaymentIntentSucceeded(payment);
            case "payment_intent.payment_failed" -> {
                if (payment.getStatus() != PaymentStatus.COMPLETED) {
                    payment.setStatus(PaymentStatus.FAILED);
                }
            }
            case "charge.refunded" -> payment.setStatus(PaymentStatus.REFUNDED);
            default -> {
                log.info("Webhook: unhandled event type {}", eventType);
                return;
            }
        }

        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        log.info("Webhook: payment {} status {}", paymentIntentId, payment.getStatus());
    }

    private void handlePaymentIntentSucceeded(Payment payment) {
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            log.info("Webhook: idempotent skip for intent {}", payment.getStripePaymentIntentId());
            return;
        }
        paymentCreditService.applyCreditViaAccountService(payment);
        payment.setStatus(PaymentStatus.COMPLETED);
    }
}
