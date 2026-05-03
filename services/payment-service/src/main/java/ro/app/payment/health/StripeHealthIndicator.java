package ro.app.payment.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import com.stripe.exception.StripeException;
import com.stripe.model.Account;

/**
 * Custom Health Indicator for Stripe API integration.
 * Checks connectivity and API key validity without hitting production.
 * 
 * Endpoint: GET /actuator/health (shows as "stripe" component)
 * Returns: UP if Stripe API is reachable, DOWN if unreachable or API key invalid
 * 
 * Importantly: This indicator does NOT affect overall application health.
 * If Stripe is DOWN, this service continues running (Stripe calls may fail gracefully).
 */
@Component
public class StripeHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(StripeHealthIndicator.class);

    @Override
    public Health health() {
        try {
            // Attempt to retrieve account metadata (no-op call that validates API key)
            // This is a safe read-only operation that verifies Stripe API connectivity
            Account account = Account.retrieve();
            
            String stripeAccountId = account.getId();
            String status = account.getChargesEnabled() ? "charges_enabled" : "charges_disabled";
            
            log.debug("Stripe health check passed. Account: {}, Status: {}", stripeAccountId, status);
            
            return Health.up()
                    .withDetail("account_id", stripeAccountId)
                    .withDetail("charges_enabled", account.getChargesEnabled())
                    .withDetail("payouts_enabled", account.getPayoutsEnabled())
                    .build();
                    
        } catch (StripeException e) {
            log.warn("Stripe health check failed: {} ({})", e.getMessage(), e.getStatusCode());
            
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("status_code", e.getStatusCode())
                    .withException(e)
                    .build();
                    
        } catch (Exception e) {
            log.error("Unexpected error in Stripe health check", e);
            
            return Health.down()
                    .withDetail("error", "Unexpected error: " + e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}
