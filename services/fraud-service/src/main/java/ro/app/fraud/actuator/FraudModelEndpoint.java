package ro.app.fraud.actuator;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import ro.app.fraud.tier3.Tier3MlService;

/**
 * Custom Actuator endpoint pentru vizibilitate operațională Tier3 ML.
 *
 * URL: GET /actuator/fraud-model
 *
 * Răspuns exemplu (model încărcat):
 * {
 *   "status": "ready",
 *   "model_type": "isolation_forest",
 *   "dataset": "PaySim (Kaggle) — sub-sampled 150k rows",
 *   "threshold": 0.58,
 *   "model_version": "paysim-v1.0",
 *   "trained_at": "2025-05-21T10:30:00Z",
 *   "details": "ML model is trained and ready for inference"
 * }
 *
 * Răspuns exemplu (model absent):
 * {
 *   "status": "model_not_found",
 *   "details": "Run training: java -jar fraud-service.jar --fraud.tier3.trainer.mode=true"
 * }
 *
 * Fix #3: Tier3MlService este @ConditionalOnProperty → poate lipsi din context
 *   (dacă fraud.tier3.ml.enabled=false). Injectare cu @Autowired(required=false)
 *   previne NoSuchBeanDefinitionException la startup.
 *
 * Fix #11: Status-ul "initializing" a fost eliminat — era înșelător (implica antrenament
 *   în background, ceea ce este fals). Noile statusuri posibile:
 *   - "ready"           → model încărcat și gata de inferență
 *   - "model_not_found" → fișierul .bin lipsește sau a eșuat la deserializare
 *   - "disabled"        → fraud.tier3.ml.enabled=false
 */
@Component
@Endpoint(id = "fraud-model")
public class FraudModelEndpoint {

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    // Fix #3: required=false — bean-ul poate lipsi dacă ML e dezactivat via config
    @Autowired(required = false)
    private Tier3MlService tier3MlService;

    @ReadOperation
    public Map<String, Object> getFraudModelStatus() {
        Map<String, Object> response = new LinkedHashMap<>(); // LinkedHashMap = ordine stabilă în JSON

        // Caz: ML dezactivat complet via fraud.tier3.ml.enabled=false
        if (tier3MlService == null) {
            response.put("status", "disabled");
            response.put("enabled", false);
            response.put("details", "ML model disabled via configuration (fraud.tier3.ml.enabled=false)");
            return response;
        }

        // Caz: model absent pe disc sau eșec la deserializare (Fix #11: "model_not_found" în loc de "initializing")
        if (!tier3MlService.isModelReady()) {
            response.put("status", "model_not_found");
            response.put("enabled", true);
            response.put("model_type", "isolation_forest");
            response.put("details",
                "Model binary not found or failed to load. " +
                "Run offline training: java -jar fraud-service.jar --fraud.tier3.trainer.mode=true");
            return response;
        }

        // Caz: model gata
        response.put("status", "ready");
        response.put("enabled", true);
        response.put("model_type", "isolation_forest");
        response.put("dataset", "PaySim (Kaggle) — sub-sampled 150k rows");
        response.put("threshold", tier3MlService.getThreshold());
        response.put("model_version", tier3MlService.getModelVersion());

        // Convertim epoch ms → ISO-8601 UTC (mai ușor de citit decât un număr brut)
        long trainedAt = tier3MlService.getModelTrainedAt();
        if (trainedAt > 0) {
            response.put("trained_at", ISO_FORMATTER.format(Instant.ofEpochMilli(trainedAt)));

            // FIX #5: Concept drift awareness — modelul antrenat pe PaySim (2017) poate
            // deveni progresiv mai putin eficient daca patternurile de frauda se schimba.
            // model_age_days este expus ca metric operational pentru echipa MLOps.
            long ageDays = (System.currentTimeMillis() - trainedAt) / (1000L * 60 * 60 * 24);
            response.put("model_age_days", ageDays);

            // Drift warning: dupa 90 zile in productie, reantrenarea este recomandata.
            // Aceasta este o limita operationala, nu o garantie statistica.
            if (ageDays > 90) {
                response.put("drift_warning",
                    String.format("Model has %d days in production (>90 days). " +
                        "Consider retraining with fresh labeled data to prevent concept drift. " +
                        "Monitor PSI (Population Stability Index) on anomaly score distribution.", ageDays));
            }
        }

        response.put("details", "ML model is trained and ready for inference on anomaly detection");
        return response;
    }
}
