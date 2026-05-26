package ro.app.fraud.tier3;

import java.time.LocalDateTime;

import ro.app.fraud.dto.FraudEvaluationRequest;
import ro.app.fraud.tier2.ScoringResult;

/**
 * Adaptor de date pentru inferenta live.
 * Construieste vectorul de features (double[6]) delegand calculele la FraudFeatureEngine,
 * aplica MinMaxScaler (FIX #3) folosind limitele salvate la antrenament in ModelSnapshot.
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
     * Construieste vectorul de features SCALAT pentru modelul de Tier 3.
     *
     * FIX #3: Aplica MinMaxScaler folosind mins/maxes din ModelSnapshot.
     * Train/inference parity: exact aceleasi transformari ca la antrenament.
     *
     * FIX #14 (partial): folosim ora din request daca este disponibila (transactionHour),
     * altfel fallback la ora serverului. Ora serverului poate fi UTC — recomandat ca
     * account-service sa transmita ora locala a tranzactiei.
     *
     * @param req      request-ul live (trebuie sa contina oldBalanceOrg pentru senderDepletionRatio)
     * @param scoring  rezultatul Tier 2 (pastrat pentru extensibilitate, neutilizat in feature vector)
     * @param snapshot modelul pre-antrenat care contine featureMins/featureMaxes pentru scaling
     * @return vector double[6] scalat la [0,1] pentru inferenta
     */
    public static double[] build(FraudEvaluationRequest req, ScoringResult scoring,
                                 ModelStore.ModelSnapshot snapshot) {
        // FIX #14: daca request-ul contine ora tranzactiei, o folosim.
        // Daca nu (field null sau 0), fallback la ora serverului (potential UTC offset).
        int hour = (req.getTransactionHour() >= 0 && req.getTransactionHour() <= 23)
                ? req.getTransactionHour()
                : LocalDateTime.now().getHour();

        double[] raw = new double[]{
            // [0] amountRatio — suma normalizata la plafonul legal Transfond (50.000 RON)
            FraudFeatureEngine.computeAmountRatio(req.getAmount(), FraudFeatureEngine.LEGAL_AMOUNT_CAP),

            // [1] typeRisk — risc bazat pe tipul tranzactiei (scara PSD2, aliniata cu PaySim)
            FraudFeatureEngine.computeTypeRiskLive(req.getTransactionType()),

            // [2] hourSuspicion — grupa de risc ciclica pe ora tranzactiei
            FraudFeatureEngine.computeHourSuspicionFromClock(hour),

            // [3] newAccountFlag — cont nou (< 30 zile) = semnal de money laundering
            FraudFeatureEngine.computeNewAccountFlagFromAge(req.getAccountAgeDays()),

            // [4] senderDepletionRatio — procentul din contul senderului golit (ATO signature)
            FraudFeatureEngine.computeSenderDepletionRatio(req.getAmount(), req.getOldBalanceOrg()),

            // [5] isRoundAmount — flag suma rotunda (specific atacurilor Cash-Out)
            FraudFeatureEngine.computeRoundAmountFlag(req.getAmount())
        };

        // FIX #3: Aplica MinMaxScaler cu limitele din snapshot (calculate pe train set).
        // CRITIC pentru train/inference parity: modelul a fost antrenat pe date scalate.
        // A pasa date nescalate la inferenta produce scoruri inconsistente.
        return MlUtils.minMaxScaleSingle(raw, snapshot.getFeatureMins(), snapshot.getFeatureMaxes());
    }

    /**
     * @deprecated Foloseste {@link #build(FraudEvaluationRequest, ScoringResult, ModelStore.ModelSnapshot)}.
     * Aceasta varianta nu aplica MinMaxScaler — pastrata temporar pentru compatibilitate.
     */
    @Deprecated(since = "v2", forRemoval = true)
    public static double[] build(FraudEvaluationRequest req, ScoringResult scoring) {
        int hour = (req.getTransactionHour() >= 0 && req.getTransactionHour() <= 23)
                ? req.getTransactionHour()
                : LocalDateTime.now().getHour();
        return new double[]{
            FraudFeatureEngine.computeAmountRatio(req.getAmount(), FraudFeatureEngine.LEGAL_AMOUNT_CAP),
            FraudFeatureEngine.computeTypeRiskLive(req.getTransactionType()),
            FraudFeatureEngine.computeHourSuspicionFromClock(hour),
            FraudFeatureEngine.computeNewAccountFlagFromAge(req.getAccountAgeDays()),
            FraudFeatureEngine.computeSenderDepletionRatio(req.getAmount(), req.getOldBalanceOrg()),
            FraudFeatureEngine.computeRoundAmountFlag(req.getAmount())
        };
    }
}
