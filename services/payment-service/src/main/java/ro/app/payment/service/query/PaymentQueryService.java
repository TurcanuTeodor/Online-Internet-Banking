package ro.app.payment.service.query;

import java.util.List;

import org.springframework.stereotype.Service;

import ro.app.payment.dto.PaymentDTO;
import ro.app.payment.dto.mapper.PaymentMapper;
import ro.app.payment.exception.ResourceNotFoundException;
import ro.app.payment.model.entity.Payment;
import ro.app.payment.repository.PaymentRepository;

/**
 * Read-side operations for payments.
 */
@Service
public class PaymentQueryService {

    private final PaymentRepository paymentRepository;

    public PaymentQueryService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentDTO getById(Long id) {
        return PaymentMapper.toDTO(requirePaymentById(id));
    }

    public List<PaymentDTO> getPaymentsByClient(Long clientId) {
        return paymentRepository.findByClientIdOrderByCreatedAtDesc(clientId)
                .stream()
                .map(PaymentMapper::toDTO)
                .toList();
    }

    /**
     * Used by refund flow and other services that need the entity.
     */
    public Payment requirePaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }
}
