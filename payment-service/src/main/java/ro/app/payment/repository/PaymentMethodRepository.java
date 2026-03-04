package ro.app.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.app.payment.model.entity.PaymentMethod;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long>{
    
    List<PaymentMethod> findByClientIdOrderByCreatedAtDesc(Long clientId);

    Optional<PaymentMethod> findByClientIdAndIsDefaultTrue(Long clientId);

    Optional<PaymentMethod> findByStripePaymentMethodId(String stripeId);
}
