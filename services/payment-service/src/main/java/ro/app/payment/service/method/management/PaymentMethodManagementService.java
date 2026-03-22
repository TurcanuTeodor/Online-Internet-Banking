package ro.app.payment.service.method.management;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.stripe.exception.StripeException;

import ro.app.payment.dto.PaymentMethodDTO;
import ro.app.payment.dto.mapper.PaymentMethodMapper;
import ro.app.payment.exception.ResourceNotFoundException;
import ro.app.payment.model.entity.PaymentMethod;
import ro.app.payment.repository.PaymentMethodRepository;

/**
 * Mutations: detach/delete in Stripe, local delete, and default-card selection.
 */
@Service
public class PaymentMethodManagementService {

    private static final Logger log = LoggerFactory.getLogger(PaymentMethodManagementService.class);

    private final PaymentMethodRepository paymentMethodRepository;

    public PaymentMethodManagementService(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    /**
     * Detach in Stripe (best-effort) and remove local row. Caller must have verified ownership.
     */
    public void deletePaymentMethod(PaymentMethod entity) {
        try {
            com.stripe.model.PaymentMethod stripePm =
                    com.stripe.model.PaymentMethod.retrieve(entity.getStripePaymentMethodId());
            stripePm.detach();
        } catch (StripeException e) {
            log.warn("Failed to detach payment method from Stripe (may already be detached): {}", e.getMessage());
        }

        paymentMethodRepository.delete(entity);
    }

    public PaymentMethodDTO setDefault(Long clientId, Long paymentMethodId) {
        PaymentMethod target = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment method not found with id: " + paymentMethodId));

        if (!target.getClientId().equals(clientId)) {
            throw new AccessDeniedException("Payment method does not belong to this client");
        }

        List<PaymentMethod> allCards = paymentMethodRepository.findByClientIdOrderByCreatedAtDesc(clientId);
        for (PaymentMethod card : allCards) {
            if (Boolean.TRUE.equals(card.getIsDefault())) {
                card.setIsDefault(false);
                paymentMethodRepository.save(card);
            }
        }

        target.setIsDefault(true);
        target = paymentMethodRepository.save(target);
        return PaymentMethodMapper.toDTO(target);
    }
}
