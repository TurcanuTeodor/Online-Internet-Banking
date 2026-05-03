package ro.app.fraud.tier3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ro.app.fraud.dto.FraudEvaluationRequest;
import ro.app.fraud.tier2.ScoringResult;

class FeatureVectorBuilderTest {

    @Test
    void build_mapsComponentScoresCorrectly() {
        FraudEvaluationRequest req = new FraudEvaluationRequest();
        req.setAmount(1000.0);
        req.setAccountAgeDays(60);

        Map<String, Double> scores = new LinkedHashMap<>();
        scores.put("amount_anomaly", 60.0);
        scores.put("frequency_anomaly", 40.0); // this key should map to freq24h
        scores.put("time_anomaly", 20.0);
        scores.put("recipient_anomaly", 80.0);
        scores.put("category_risk", 30.0);
        scores.put("velocity_24h", 50.0); // unused key, ensuring it's not mistakenly used

        ScoringResult scoring = new ScoringResult(55.0, scores, "test");

        double[] vector = FeatureVectorBuilder.build(req, scoring);

        assertEquals(6, vector.length);
        assertTrue(vector[0] >= 0 && vector[0] <= 1.0); // amount_ratio = 1000/5000 = 0.2
        assertEquals(0.2, vector[0], 0.01);
        assertEquals(0.55, vector[1], 0.01); // tier2_norm = 55/100
        assertEquals(0.40, vector[2], 0.01); // freq24h = 40/100 (frequency_anomaly)
        assertEquals(1.0, vector[3], 0.01); // newRecipient: 80 > 50 -> 1.0
        assertEquals(0.20, vector[4], 0.01); // time_anomaly = 20/100 -> 0.2
        assertEquals(0.0, vector[5], 0.01); // accountAgeDays = 60 >= 30 -> 0.0
    }
}
