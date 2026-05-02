package ro.app.fraud.tier3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import ro.app.fraud.dto.FraudEvaluationRequest;
import ro.app.fraud.tier2.ScoringResult;
import smile.anomaly.IsolationForest;

@Service
@ConditionalOnProperty(name = "fraud.tier3.ml.enabled", havingValue = "true", matchIfMissing = true)
public class Tier3MlService {

    private static final Logger log = LoggerFactory.getLogger(Tier3MlService.class);
    private static final String MODEL_VERSION = "isolation-forest-v1.0-seed";

    @Value("${fraud.tier3.ml.contamination:0.05}")
    private double contamination;
    @Value("${fraud.tier3.ml.seed:42}")
    private int seed;
    @Value("${fraud.tier3.ml.threshold:0.62}")
    private double threshold;
    @Value("${fraud.tier3.ml.training-samples:1000}")
    private int trainingSamples;

    private IsolationForest model;
    private double[] featureMeans; // stored for perturbation method

    @PostConstruct // App starts → @PostConstruct fires → trainModel() runs → model is ready
    void trainModel() {

        int normalCount = (int) (trainingSamples * (1 - contamination)); // 950
        int anomalyCount = trainingSamples - normalCount; // 50

        double[][] data = TrainingDataGenerator.generate(normalCount, anomalyCount, seed);
        featureMeans = MlUtils.computeMeans(data);

        model = IsolationForest.fit(data, 100, 256, contamination, 0);

        log.info("Tier3-ML model trained: version={}{} samples={} normal={} anomalies={} threshold={}",
                MODEL_VERSION, seed, trainingSamples, normalCount, anomalyCount, threshold);

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

}
