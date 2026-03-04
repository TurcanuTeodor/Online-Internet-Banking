package ro.app.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.app.payment.model.entity.Payment;
import ro.app.payment.model.enums.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByClientIdOrderByCreatedAtDesc(Long clientId);

    Optional<Payment> findByStripePaymentIntentId(String intentId);

    List<Payment> findByStatus(PaymentStatus status);

}
