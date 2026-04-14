package ro.app.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.app.payment.model.entity.StripeCustomer;

public interface StripeCustomerRepository extends JpaRepository<StripeCustomer, Long> {
}

