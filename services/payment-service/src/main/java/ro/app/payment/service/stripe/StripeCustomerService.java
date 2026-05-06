package ro.app.payment.service.stripe;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;

import ro.app.payment.exception.PaymentFailedException;
import ro.app.payment.model.entity.StripeCustomer;
import ro.app.payment.repository.StripeCustomerRepository;

@Service
public class StripeCustomerService {

    private static final Logger log = LoggerFactory.getLogger(StripeCustomerService.class);

    private final StripeCustomerRepository stripeCustomerRepository;

    public StripeCustomerService(StripeCustomerRepository stripeCustomerRepository) {
        this.stripeCustomerRepository = stripeCustomerRepository;
    }

    /**
     * Returns a stable Stripe Customer id for an internal client.
     * Creates the Customer on-demand (Stripe test/live mode depends on API key).
     * 
     * Metrics: stripe.customer.create latency and success/failure counts
     */
    @Transactional
    public String getOrCreateCustomerId(Long clientId) {
        if (clientId == null) throw new IllegalArgumentException("clientId is required");

        return stripeCustomerRepository.findById(clientId)
                .map(StripeCustomer::getStripeCustomerId)
                .orElseGet(() -> createCustomer(clientId));
    }

    private String createCustomer(Long clientId) {
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setDescription("CashTactics client " + clientId)
                    // keep linkage in metadata (useful for debugging)
                    .setMetadata(Map.of("clientId", String.valueOf(clientId)))
                    .build();

            Customer customer = Customer.create(params);
            if (customer == null || customer.getId() == null || customer.getId().isBlank()) {
                throw new PaymentFailedException("Stripe did not return a customer id");
            }

            StripeCustomer entity = new StripeCustomer();
            entity.setClientId(clientId);
            entity.setStripeCustomerId(customer.getId());
            stripeCustomerRepository.save(entity);

            return customer.getId();
        } catch (StripeException e) {
            log.error("Failed to create Stripe customer for client {}: {}", clientId, e.getMessage());
            throw new PaymentFailedException("Failed to create Stripe customer: " + e.getMessage(), e);
        }
    }
}

