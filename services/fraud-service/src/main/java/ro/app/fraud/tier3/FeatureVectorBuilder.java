package ro.app.fraud.tier3;

import ro.app.fraud.dto.FraudEvaluationRequest;
import ro.app.fraud.tier2.ScoringResult;

/**
 * Adaptor de date pentru inferență live.
 * Construiește vectorul de features (double[6]) delegând calculele la FraudFeatureEngine.
 * Balanțele nu sunt transmise în request, deci se folosește valoarea neutră.
 */
public final class FeatureVectorBuilder {

    private FeatureVectorBuilder() {
    }

    /**
     * Construiește vectorul de features pentru modelul de Tier 3.
     *
     * @param req     request-ul live
     * @param scoring rezultatul Tier 2 (păstrat pentru extensibilitate)
     * @return vector double[6] pentru inferență
     */
    public static double[] build(FraudEvaluationRequest req, ScoringResult scoring) {
        return new double[]{
            // [0] amountRatio (normalizat)
            FraudFeatureEngine.computeAmountRatio(req.getAmount(), FraudFeatureEngine.LIVE_AMOUNT_CAP),

            // [1] balance sender (necunoscut) → neutral
            FraudFeatureEngine.NEUTRAL,

            // [2] balance dest (necunoscut) → neutral
            FraudFeatureEngine.NEUTRAL,

            // [3] typeRisk (bazat pe tip tranzacție)
            FraudFeatureEngine.computeTypeRiskLive(req.getTransactionType()),

            // [4] hourSuspicion (noapte)
            FraudFeatureEngine.computeHourSuspicionFromClock(),

            // [5] newAccountFlag (cont nou)
            FraudFeatureEngine.computeNewAccountFlagFromAge(req.getAccountAgeDays())
        };
    }
}
