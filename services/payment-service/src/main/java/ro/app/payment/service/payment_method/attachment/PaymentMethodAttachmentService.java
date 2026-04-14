package ro.app.payment.service.payment_method.attachment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.stripe.exception.StripeException;
import com.stripe.param.PaymentMethodAttachParams;

import ro.app.payment.dto.PaymentMethodDTO;
import ro.app.payment.dto.mapper.PaymentMethodMapper;
import ro.app.payment.dto.request.AttachPaymentMethodRequest;
import ro.app.payment.exception.PaymentFailedException;
import ro.app.payment.model.entity.PaymentMethod;
import ro.app.payment.repository.PaymentMethodRepository;
import ro.app.payment.service.stripe.StripeCustomerService;

/**
 * Attaches a Stripe PaymentMethod to a client: retrieves card metadata from Stripe and persists locally.
 */
@Service
public class PaymentMethodAttachmentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentMethodAttachmentService.class);

    private final PaymentMethodRepository paymentMethodRepository;
    private final StripeCustomerService stripeCustomerService;

    public PaymentMethodAttachmentService(
            PaymentMethodRepository paymentMethodRepository,
            StripeCustomerService stripeCustomerService) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.stripeCustomerService = stripeCustomerService;
    }

    public PaymentMethodDTO attachPaymentMethod(AttachPaymentMethodRequest req) {
        try {
            com.stripe.model.PaymentMethod stripePm =
                    com.stripe.model.PaymentMethod.retrieve(req.getStripePaymentMethodId());

            // Ensure the PM is attached to a Stripe Customer so it can be reused across multiple PaymentIntents.
            // Without this, Stripe may allow a one-time use and then fail with "previously used without Customer attachment".
            String customerId = stripeCustomerService.getOrCreateCustomerId(req.getClientId());
            stripePm.attach(PaymentMethodAttachParams.builder().setCustomer(customerId).build());

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
