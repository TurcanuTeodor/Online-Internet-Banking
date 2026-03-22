package ro.app.payment.service.payment_method.query;

import java.util.List;

import org.springframework.stereotype.Service;

import ro.app.payment.dto.PaymentMethodDTO;
import ro.app.payment.dto.mapper.PaymentMethodMapper;
import ro.app.payment.exception.ResourceNotFoundException;
import ro.app.payment.model.entity.PaymentMethod;
import ro.app.payment.repository.PaymentMethodRepository;

/**
 * Read-side: list saved cards and load entities for ownership checks.
 */
@Service
public class PaymentMethodQueryService {

    private final PaymentMethodRepository paymentMethodRepository;

    public PaymentMethodQueryService(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    public List<PaymentMethodDTO> getByClient(Long clientId) {
        return paymentMethodRepository.findByClientIdOrderByCreatedAtDesc(clientId)
                .stream()
                .map(PaymentMethodMapper::toDTO)
                .toList();
    }

    public PaymentMethod requireById(Long id) {
        return paymentMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment method not found with id: " + id));
    }
}
