package ro.app.fraud.tier3;

import java.time.LocalDateTime;

import ro.app.fraud.dto.FraudEvaluationRequest;
import ro.app.fraud.tier2.ScoringResult;

/**
 * Adaptor de date pentru inferenta live.
 * Construieste vectorul de features (double[6]) delegand calculele la FraudFeatureEngine.
 *
 * STRATEGIE: Open Payments / PSD2 — Behavioral Fraud Detector
 * Vectorul este identic cu cel din PaySimFeatureMapper:
 *   [amountRatio, typeRisk, hourSuspicion, newAccountFlag, senderDepletionRatio, isRoundAmount]
 *
 * Soldul destinatarului NU este inclus — inaccesibil la transferuri externe (Off-Us).
 * Soldul expeditorului (oldBalanceOrg) este disponibil mereu (clientul nostru).
 */
public final class FeatureVectorBuilder {

    private FeatureVectorBuilder() {
    }

    /**
     * Construieste vectorul de features pentru modelul de Tier 3.
     *
     * @param req     request-ul live (trebuie sa contina oldBalanceOrg pentru senderDepletionRatio)
     * @param scoring rezultatul Tier 2 (pastrat pentru extensibilitate)
     * @return vector double[6] pentru inferenta
     */
    public static double[] build(FraudEvaluationRequest req, ScoringResult scoring) {
        int hour = LocalDateTime.now().getHour();
        return new double[]{
            // [0] amountRatio — suma normalizata la plafonul legal Transfond (50.000 RON)
            FraudFeatureEngine.computeAmountRatio(req.getAmount(), FraudFeatureEngine.LEGAL_AMOUNT_CAP),

            // [1] typeRisk — risc bazat pe tipul tranzactiei (scara PSD2)
            FraudFeatureEngine.computeTypeRiskLive(req.getTransactionType()),

            // [2] hourSuspicion — grupa de risc ciclica pe ora curenta a zilei
            FraudFeatureEngine.computeHourSuspicionFromClock(hour),

            // [3] newAccountFlag — cont nou (< 30 zile) = semnal de money laundering
            FraudFeatureEngine.computeNewAccountFlagFromAge(req.getAccountAgeDays()),

            // [4] senderDepletionRatio — procentul din contul senderului golit (ATO signature)
            FraudFeatureEngine.computeSenderDepletionRatio(req.getAmount(), req.getOldBalanceOrg()),

            // [5] isRoundAmount — flag suma rotunda (specific atacurilor Cash-Out)
            FraudFeatureEngine.computeRoundAmountFlag(req.getAmount())
        };
    }
}
