package ro.app.fraud.tier3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import ro.app.fraud.config.FraudProperties;
import ro.app.fraud.dto.FraudEvaluationRequest;
import ro.app.fraud.tier2.ScoringResult;

/**
 * =====================================================================
 * Tier3MlService — Motor de Inferență ML (Refactorizat)
 * =====================================================================
 *
 * DIFERENȚA față de versiunea anterioară:
 * ----------------------------------------
 * ÎNAINTE: @PostConstruct antrena modelul de la zero la fiecare pornire
 *           folosind TrainingDataGenerator (date sintetice random).
 *           Probleme: cold start, date nerealiste, nereproductibil.
 *
 * ACUM:    @PostConstruct NUMAI deserializează modelul pre-antrenat
 *           din fișierul isolation_forest_model.bin de pe disc.
 *           Antrenamentul se face OFFLINE, o singură dată, prin ModelTrainerCli.
 *
 * FAIL-SAFE:
 * -----------
 * Dacă modelul nu există pe disc (ex: prima rulare, mediu nou),
 * serviciul pornește cu model=null și logează un avertisment clar.
 * Metoda analyze() returnează MlVerdict ALLOW → nu blochează tranzacțiile.
 * Aceasta este o decizie de design "fail-open" conștientă pentru Tier 3
 * (care este oricum asincron și post-hoc).
 */
@Service
@ConditionalOnProperty(name = "fraud.tier3.ml.enabled", havingValue = "true", matchIfMissing = true)
public class Tier3MlService {

    private static final Logger log = LoggerFactory.getLogger(Tier3MlService.class);

    private final String modelPath;
    private final double configuredThreshold;

    // State încărcat din fișierul .bin
    private ModelStore.ModelSnapshot snapshot; // null dacă modelul nu a fost găsit

    public Tier3MlService(FraudProperties fraudProperties) {
        FraudProperties.Tier3 tier3 = fraudProperties.getTier3();
        this.modelPath           = tier3.getModelPath();
        this.configuredThreshold = tier3.getMlThreshold();
    }

    /**
     * La pornirea aplicației: încearcă să încarce modelul din disc.
     * NU mai antrenează nimic. Dacă modelul lipsește → pornire degradată.
     */
    @PostConstruct
    void loadModel() {
        if (!ModelStore.exists(modelPath)) {
            log.warn("Tier3 model not found: {}", modelPath);
            log.warn("Run offline training: java -jar fraud-service.jar --fraud.tier3.trainer.mode=true");
            log.warn("Tier3 running in DEGRADED mode (all transactions → ALLOW)");
            this.snapshot = null;
            return;
        }

        try {
            this.snapshot = ModelStore.load(modelPath);
            log.info("Tier3-ML model loaded: version={} threshold={}",
                    snapshot.version, snapshot.threshold);
        } catch (Exception e) {
            log.error("Error loading Tier3 model: {}", e.getMessage());
            this.snapshot = null;
        }
    }

    /**
     * Analizează o tranzacție live și returnează verdictul ML.
     *
     * @param decisionId ID-ul deciziei din BD (pentru logging corelat)
     * @param req        request-ul live cu datele tranzacției
     * @param scoring    rezultatul Tier 2 (folosit pentru features)
     * @return MlVerdict cu ALLOW sau FLAG + explicație
     */
    public MlVerdict analyze(Long decisionId, FraudEvaluationRequest req, ScoringResult scoring) {
        // Dacă modelul nu a putut fi încărcat → fail-open (ALLOW)
        if (snapshot == null) {
            log.debug("Tier3 model not found for decision: {}", decisionId);
            return new MlVerdict("ALLOW", 0.0, "Model not loaded, default ALLOW");
        }

        // Feature engineering: request live → vector numeric (via FeatureVectorBuilder)
        double[] features = FeatureVectorBuilder.build(req, scoring);

        // Scorul de anomalie: [0, 1]. Mai aproape de 1 = mai suspect.
        double anomalyScore = snapshot.model.score(features);

        // Importanțele feature-urilor via perturbation method
        double[] importances = PerturbationAnalyzer.computeFeatureImportances(
                features, snapshot.model, snapshot.getFeatureMeans());

        // Decizie binară bazată pe threshold calibrat offline
        double  activeThreshold = snapshot.threshold;
        boolean flagged = anomalyScore > activeThreshold;
        String  reasoning = ReasoningBuilder.build(flagged, anomalyScore, importances);
        double  confidence = Math.min(1.0, Math.abs(anomalyScore - activeThreshold) * 2.0);

        log.info("Tier3-ML: decisionId={} score={} threshold={} verdict={}",
                decisionId, String.format("%.4f", anomalyScore), activeThreshold,
                flagged ? "FLAG" : "ALLOW");

        return new MlVerdict(flagged ? "FLAG" : "ALLOW", confidence, reasoning);
    }

    // ── Accessors pentru Actuator / Health Check ─────────────────────────────

    public boolean isModelReady() {
        return snapshot != null;
    }

    public boolean isEnabled() {
        return true; // Bean există doar dacă fraud.tier3.ml.enabled=true
    }

    public double getThreshold() {
        return snapshot != null ? snapshot.threshold : configuredThreshold;
    }

    public String getModelVersion() {
        return snapshot != null ? snapshot.version : "NOT_LOADED";
    }

    public long getModelTrainedAt() {
        return snapshot != null ? snapshot.trainedAtEpoch : 0L;
    }
}
