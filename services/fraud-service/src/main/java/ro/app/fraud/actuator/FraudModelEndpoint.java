package ro.app.fraud.actuator;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import ro.app.fraud.tier3.Tier3MlService;

/**
 * Custom Actuator endpoint for Tier3 ML Model operational visibility.
 * 
 * Endpoint: GET /actuator/fraud-model
 * Purpose: Expose ML model metadata (version, status, thresholds, training config)
 * 
 * Thesis value: Demonstrates operational/MLOps concerns in production microservices —
 * the ability to inspect ML component health and configuration without code inspection.
 * This is critical for maintaining ML models in production.
 * 
 * Example response:
 * {
 *   "status": "ready",
 *   "enabled": true,
 *   "model_type": "isolation_forest",
 *   "threshold": 0.62,
 *   "training_samples": 1000,
 *   "contamination": 0.05,
 *   "seed": 42,
 *   "details": "ML model is trained and ready for inference"
 * }
 */
@Component
@Endpoint(id = "fraud-model")
public class FraudModelEndpoint {

    private final Tier3MlService tier3MlService;

    public FraudModelEndpoint(Tier3MlService tier3MlService) {
        this.tier3MlService = tier3MlService;
    }

    @ReadOperation
    public Map<String, Object> getFraudModelStatus() {
        Map<String, Object> response = new HashMap<>();

        // Status field
        String status = determineStatus();
        response.put("status", status);

        // Core configuration
        response.put("enabled", tier3MlService.isEnabled());
        response.put("model_type", "isolation_forest");

        // Model parameters
        response.put("threshold", tier3MlService.getThreshold());
        response.put("training_samples", tier3MlService.getTrainingSamples());
        response.put("contamination", tier3MlService.getContamination());
        response.put("seed", tier3MlService.getSeed());

        // Human-readable details
        response.put("details", generateDetails(status));

        return response;
    }

    private String determineStatus() {
        if (!tier3MlService.isEnabled()) {
            return "disabled";
        }
        if (!tier3MlService.isModelReady()) {
            return "initializing";
        }
        return "ready";
    }

    private String generateDetails(String status) {
        switch (status) {
            case "disabled":
                return "ML model is disabled via configuration (fraud.tier3.ml.enabled=false)";
            case "initializing":
                return "ML model training in progress or failed; not yet ready for inference";
            case "ready":
                return "ML model is trained and ready for inference on anomaly detection";
            default:
                return "Unknown status";
        }
    }
}
