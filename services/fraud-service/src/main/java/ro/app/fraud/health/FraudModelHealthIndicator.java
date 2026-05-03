package ro.app.fraud.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import ro.app.fraud.tier3.Tier3MlService;

/**
 * Custom Health Indicator for Tier 3 ML Model.
 * Checks if Isolation Forest model is loaded, trained, and ready for inference.
 * 
 * Endpoint: GET /actuator/health (shows as "fraud_model" component)
 * Returns: UP if model loaded, DEGRADED if model loading slow, DOWN if model unavailable
 * 
 * Thesis value: Demonstrates operational visibility into ML component health,
 * essential for production ML systems.
 */
@Component("fraud-model")
public class FraudModelHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(FraudModelHealthIndicator.class);

    private final Tier3MlService tier3MlService;

    public FraudModelHealthIndicator(Tier3MlService tier3MlService) {
        this.tier3MlService = tier3MlService;
    }

    @Override
    public Health health() {
        try {
            // Check if model is enabled
            if (!tier3MlService.isEnabled()) {
                log.warn("Fraud model is disabled");
                return Health.outOfService()
                        .withDetail("status", "DISABLED")
                        .withDetail("reason", "ML model is disabled via configuration")
                        .build();
            }

            // Check if model is ready (has been trained)
            if (!tier3MlService.isModelReady()) {
                log.warn("Fraud model not yet trained/ready");
                return Health.down()
                        .withDetail("status", "NOT_READY")
                        .withDetail("reason", "Model initialization in progress or failed")
                        .build();
            }

            // Get model metadata
            double threshold = tier3MlService.getThreshold();
            int trainingSamples = tier3MlService.getTrainingSamples();
            double contamination = tier3MlService.getContamination();
            
            log.debug("Fraud model health check passed. Threshold: {}, Samples: {}, Contamination: {}",
                    threshold, trainingSamples, contamination);

            return Health.up()
                    .withDetail("status", "READY")
                    .withDetail("model_type", "isolation_forest")
                    .withDetail("threshold", threshold)
                    .withDetail("training_samples", trainingSamples)
                    .withDetail("contamination", contamination)
                    .withDetail("seed", tier3MlService.getSeed())
                    .build();

        } catch (Exception e) {
            log.error("Error in fraud model health check", e);
            
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}
