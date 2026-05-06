package ro.app.fraud.tier3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import ro.app.fraud.config.FraudProperties;
import ro.app.fraud.dto.FraudEvaluationRequest;
import ro.app.fraud.tier2.ScoringResult;
import smile.anomaly.IsolationForest;

@Service
@ConditionalOnProperty(name = "fraud.tier3.ml.enabled", havingValue = "true", matchIfMissing = true)
public class Tier3MlService {

    private static final Logger log = LoggerFactory.getLogger(Tier3MlService.class);
    private static final String MODEL_VERSION = "isolation-forest-v1.0-seed";

    private final double contamination;
    private final int seed;
    private double threshold;
    private final int trainingSamples;

    private IsolationForest model;
    private double[] featureMeans; // stored for perturbation method

    /**
     * Dependency injection of FraudProperties for centralized ML configuration.
     * Extracts Tier3-specific parameters in constructor for clarity and testability.
     */
    public Tier3MlService(FraudProperties fraudProperties) {
        FraudProperties.Tier3 tier3Config = fraudProperties.getTier3();
        this.contamination = tier3Config.getMlContamination();
        this.seed = tier3Config.getMlSeed();
        this.threshold = tier3Config.getMlThreshold();
        this.trainingSamples = tier3Config.getMlTrainingSamples();
    }

    @PostConstruct // App starts → @PostConstruct fires → trainModel() runs → model is ready
    void trainModel() {
        int normalCount = (int) (trainingSamples * (1 - contamination)); // 950
        int anomalyCount = trainingSamples - normalCount; // 50

        double[][] data = TrainingDataGenerator.generate(normalCount, anomalyCount, seed);
        
        // PENTRU PERTURBARE - calculăm media DOAR pe datele normale, evitând contaminarea
        double[][] normalData = java.util.Arrays.copyOfRange(data, 0, normalCount);
        featureMeans = MlUtils.computeMeans(normalData);

        model = IsolationForest.fit(data, 100, 256, contamination, 0);

        log.info("Tier3-ML model trained: version={}{} samples={} normal={} anomalies={} initial_threshold={}",
                MODEL_VERSION, seed, trainingSamples, normalCount, anomalyCount, threshold);

        // Evaluarea pe pragul fix initial
        evaluateModel(data, normalCount);
        
        // Calibrarea pragului prin maximizarea scorului F1
        this.threshold = findOptimalThreshold(data, normalCount);
    }

    public MlVerdict analyze(Long decisionId, FraudEvaluationRequest req, ScoringResult scoring) {

        double[] features = FeatureVectorBuilder.build(req, scoring);
        double anomalyScore = model.score(features);

        // feature importance via perturbation method
        double[] importances = PerturbationAnalyzer.computeFeatureImportances(features, model, featureMeans);

        boolean flagged = anomalyScore > threshold;
        String reasoning = ReasoningBuilder.build(flagged, anomalyScore, importances);
        double confidence = Math.min(1.0, Math.abs(anomalyScore - 0.5) * 2.0); // simple confidence heuristic

        log.info("Tier3-ML: decisionId={} score={} threshold={} verdict={}",
                decisionId, String.format("%.4f", anomalyScore), threshold, flagged ? "FLAG" : "ALLOW");

        return new MlVerdict(flagged ? "FLAG" : "ALLOW", confidence, reasoning);
    }

    private void evaluateModel(double[][] testData, int normalCount) {
        int tp = 0, fp = 0, tn = 0, fn = 0;
        for (int i = 0; i < testData.length; i++) {
            boolean actualFraud = i >= normalCount;
            boolean predicted   = model.score(testData[i]) > threshold;
            if (predicted && actualFraud)  tp++;
            if (predicted && !actualFraud) fp++;
            if (!predicted && !actualFraud) tn++;
            if (!predicted && actualFraud)  fn++;
        }
        double precision = tp + fp > 0 ? (double) tp / (tp + fp) : 0;
        double recall    = tp + fn > 0 ? (double) tp / (tp + fn) : 0;
        double f1        = precision + recall > 0 ? 2 * precision * recall / (precision + recall) : 0;
        log.info("Tier3-ML Evaluation (initial threshold {}): precision={} recall={} f1={}", threshold, precision, recall, f1);
    }

    private double findOptimalThreshold(double[][] testData, int normalCount) {
        double bestF1 = 0, bestThreshold = 0.5;
        for (double t = 0.40; t <= 0.90; t += 0.05) {
            int tp = 0, fp = 0, fn = 0;
            for (int i = 0; i < testData.length; i++) {
                boolean actualFraud = i >= normalCount;
                boolean predicted   = model.score(testData[i]) > t;
                if (predicted && actualFraud)  tp++;
                if (predicted && !actualFraud) fp++;
                if (!predicted && actualFraud)  fn++;
            }
            double precision = tp + fp > 0 ? (double) tp / (tp + fp) : 0;
            double recall    = tp + fn > 0 ? (double) tp / (tp + fn) : 0;
            double f1        = precision + recall > 0 ? 2 * precision * recall / (precision + recall) : 0;
            
            if (f1 > bestF1) { 
                bestF1 = f1; 
                bestThreshold = t; 
            }
        }
        log.info("Optimal threshold calibrated: {} with max F1={}", bestThreshold, bestF1);
        return bestThreshold;
    }

    // ============ PUBLIC ACCESSORS FOR HEALTH CHECK & METRICS ============

    /**
     * Check if ML model is trained and ready for inference
     */
    public boolean isModelReady() {
        return model != null && featureMeans != null;
    }

    /**
     * ML model is enabled via @ConditionalOnProperty or explicitly check
     */
    public boolean isEnabled() {
        return true; // Service only instantiates if fraud.tier3.ml.enabled=true
    }

    public double getThreshold() {
        return threshold;
    }

    public int getTrainingSamples() {
        return trainingSamples;
    }

    public double getContamination() {
        return contamination;
    }

    public int getSeed() {
        return seed;
    }

}
