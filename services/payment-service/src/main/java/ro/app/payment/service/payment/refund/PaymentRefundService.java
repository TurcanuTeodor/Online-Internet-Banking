package ro.app.payment.service.payment.refund;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.micrometer.observation.annotation.Observed;

import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;

import ro.app.payment.dto.PaymentDTO;
import ro.app.payment.dto.mapper.PaymentMapper;
import ro.app.payment.exception.PaymentFailedException;
import ro.app.payment.model.entity.Payment;
import ro.app.payment.model.enums.PaymentStatus;
import ro.app.payment.repository.PaymentRepository;
import ro.app.payment.service.payment.query.PaymentQueryService;

/**
 * Stripe refunds and local status updates.
 */
@Service
public class PaymentRefundService {

    private static final Logger log = LoggerFactory.getLogger(PaymentRefundService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentQueryService paymentQueryService;

    public PaymentRefundService(PaymentRepository paymentRepository, PaymentQueryService paymentQueryService) {
        this.paymentRepository = paymentRepository;
        this.paymentQueryService = paymentQueryService;
    }

    @Observed(name = "stripe.refund.create", contextualName = "refund")
    public PaymentDTO refundPayment(Long id) {
        Payment payment = paymentQueryService.requirePaymentById(id);

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new PaymentFailedException("Only completed payments can be refunded. Current status: " + payment.getStatus());
        }

        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(payment.getStripePaymentIntentId())
                    .build();

            Refund.create(params);

            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setUpdatedAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);
        } catch (StripeException e) {
            log.error("Stripe refund failed for payment {}: {}", id, e.getMessage());
            throw new PaymentFailedException("Refund failed: " + e.getMessage(), e);
        }

        return PaymentMapper.toDTO(payment);
    }
}
