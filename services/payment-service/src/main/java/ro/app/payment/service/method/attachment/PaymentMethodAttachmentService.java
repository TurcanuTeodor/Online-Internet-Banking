package ro.app.payment.service.method.attachment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.stripe.exception.StripeException;

import ro.app.payment.dto.PaymentMethodDTO;
import ro.app.payment.dto.mapper.PaymentMethodMapper;
import ro.app.payment.dto.request.AttachPaymentMethodRequest;
import ro.app.payment.exception.PaymentFailedException;
import ro.app.payment.model.entity.PaymentMethod;
import ro.app.payment.repository.PaymentMethodRepository;

/**
 * Attaches a Stripe PaymentMethod to a client: retrieves card metadata from Stripe and persists locally.
 */
@Service
public class PaymentMethodAttachmentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentMethodAttachmentService.class);

    private final PaymentMethodRepository paymentMethodRepository;

    public PaymentMethodAttachmentService(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    public PaymentMethodDTO attachPaymentMethod(AttachPaymentMethodRequest req) {
        try {
            com.stripe.model.PaymentMethod stripePm =
                    com.stripe.model.PaymentMethod.retrieve(req.getStripePaymentMethodId());

            PaymentMethod entity = new PaymentMethod();
            entity.setClientId(req.getClientId());
            entity.setStripePaymentMethodId(req.getStripePaymentMethodId());

            if (stripePm.getCard() != null) {
                entity.setCardBrand(stripePm.getCard().getBrand());
                entity.setCardLast4(stripePm.getCard().getLast4());
                entity.setExpiryMonth(stripePm.getCard().getExpMonth().intValue());
                entity.setExpiryYear(stripePm.getCard().getExpYear().intValue());
            }

            boolean hasExisting = !paymentMethodRepository
                    .findByClientIdOrderByCreatedAtDesc(req.getClientId()).isEmpty();
            entity.setIsDefault(!hasExisting);

            entity = paymentMethodRepository.save(entity);
            return PaymentMethodMapper.toDTO(entity);

        } catch (StripeException e) {
            log.error("Failed to retrieve payment method from Stripe: {}", e.getMessage());
            throw new PaymentFailedException("Failed to attach payment method: " + e.getMessage(), e);
        }
    }
}
