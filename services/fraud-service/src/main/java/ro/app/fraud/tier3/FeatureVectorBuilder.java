package ro.app.fraud.tier3;

import ro.app.fraud.dto.FraudEvaluationRequest;
import ro.app.fraud.tier2.ScoringResult;

// before the ML model can analyze a transaction, we need to convert it into a vector of numbers (features)
// it takes the raw transaction data and the scoring results from Tier 2
public final class FeatureVectorBuilder {

    private FeatureVectorBuilder() {}

    public static double[] build(FraudEvaluationRequest req, ScoringResult scoring){

        double amountRatio = Math.min(1.0, req.getAmount() / 5000.0); // cap at 1.0 for amounts >= 5000
        double tier2Norm = scoring.totalScore() / 100.0; // normalize to [0,1]  
        double freq24h = Math.min(1.0, scoring.componentScores()
                    .getOrDefault("frequency_anomaly", 0.0) / 100.0); // normalize and cap
        double newRecipient = scoring.componentScores()
                    .getOrDefault("recipient_anomaly", 0.0) > 50.0 ? 1.0 : 0.0; // binary feature based on threshold
        double hourDeviation = Math.min(1.0, scoring.componentScores()
                    .getOrDefault("time_anomaly", 0.0) / 100.0); // normalize and cap
        double newAccount = req.getAccountAgeDays() < 30 ? 1.0 : 0.0; // binary feature for new accounts
                    
        return new double[]{amountRatio, tier2Norm, freq24h, newRecipient, hourDeviation, newAccount};
    }
}
