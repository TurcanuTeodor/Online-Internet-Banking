package ro.app.payment.service.payment.settlement;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

import ro.app.payment.exception.PaymentFailedException;
import ro.app.payment.model.entity.Payment;
import ro.app.payment.model.enums.PaymentStatus;
import ro.app.payment.repository.PaymentRepository;
import ro.app.payment.security.JwtPrincipal;
import ro.app.payment.security.OwnershipChecker;
import ro.app.payment.service.payment.webhook.PaymentWebhookService;

/**
 * Local/dev convenience: confirm + settle a succeeded top-up without waiting for webhooks.
 * Idempotent: calling multiple times won't double-credit.
 */
@Service
public class PaymentSettlementService {

    private static final Logger log = LoggerFactory.getLogger(PaymentSettlementService.class);

    private final PaymentRepository paymentRepository;
    private final OwnershipChecker ownershipChecker;
    private final PaymentWebhookService paymentWebhookService;

    public PaymentSettlementService(
            PaymentRepository paymentRepository,
            OwnershipChecker ownershipChecker,
            PaymentWebhookService paymentWebhookService) {
        this.paymentRepository = paymentRepository;
        this.ownershipChecker = ownershipChecker;
        this.paymentWebhookService = paymentWebhookService;
    }

    @Transactional
    public void confirmTopUpSucceeded(String stripePaymentIntentId, JwtPrincipal principal) {
        if (stripePaymentIntentId == null || stripePaymentIntentId.isBlank()) {
            throw new IllegalArgumentException("stripePaymentIntentId is required");
        }

        Payment payment = paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId)
                .orElseThrow(() -> new PaymentFailedException("No payment found for intent " + stripePaymentIntentId));

        ownershipChecker.checkOwnership(principal, payment.getClientId());

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return; // idempotent
        }

        try {
            PaymentIntent intent = PaymentIntent.retrieve(stripePaymentIntentId);
            if (!"succeeded".equals(intent.getStatus())) {
                throw new PaymentFailedException("Payment is not settled yet (status: " + intent.getStatus() + ")");
            }
        } catch (StripeException e) {
            log.error("Stripe retrieve intent failed: {}", e.getMessage());
            throw new PaymentFailedException("Could not verify payment with Stripe: " + e.getMessage(), e);
        }

        paymentWebhookService.settleSucceededIntent(payment);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }
}

