package ro.app.fraud.tier3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import ro.app.fraud.dto.FraudEvaluationRequest;
import ro.app.fraud.tier2.ScoringResult;

/**
 * Teste pentru FeatureVectorBuilder (după refactorizare cu FraudFeatureEngine).
 *
 * Testăm că:
 *   1. Vectorul are exact 6 dimensiuni
 *   2. Fiecare feature este calculat corect conform FraudFeatureEngine
 *   3. Valorile neutre (balanceDeltaOrg, balanceDeltaDest) sunt 0.5
 */
class FeatureVectorBuilderTest {

    /**
     * Request de bază: sumă 1000, cont vechi (60 zile), tip TRANSFER_EXTERNAL.
     * Toate valorile de zi (nu noapte) → hourSuspicion = 0.0 sau 1.0 în funcție de ora serverului.
     */
    @Test
    void build_producesSixDimensionalVector() {
        FraudEvaluationRequest req = new FraudEvaluationRequest();
        req.setAmount(1000.0);
        req.setAccountAgeDays(60);
        req.setTransactionType("TRANSFER_EXTERNAL");

        ScoringResult scoring = new ScoringResult(55.0, Collections.emptyMap(), "test");

        double[] vector = FeatureVectorBuilder.build(req, scoring);

        assertEquals(6, vector.length, "Vectorul trebuie să aibă exact 6 dimensiuni");
    }

    @Test
    void build_amountRatio_isNormalizedWithLiveCap() {
        // 1000 / 5000 = 0.2
        FraudEvaluationRequest req = buildRequest(1000.0, 60, null);
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(0.2, vector[0], 0.001, "amountRatio: 1000/5000 = 0.2");
    }

    @Test
    void build_amountRatio_isCappedAtOne() {
        // 10000 / 5000 = 2.0 → capped la 1.0
        FraudEvaluationRequest req = buildRequest(10_000.0, 60, null);
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(1.0, vector[0], 0.001, "amountRatio: cap la 1.0 pentru sume mari");
    }

    @Test
    void build_balanceDeltaOrg_isNeutral() {
        // Balanțele nu sunt disponibile în context live → NEUTRAL = 0.5
        FraudEvaluationRequest req = buildRequest(1000.0, 60, null);
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(FraudFeatureEngine.NEUTRAL, vector[1], 0.001,
                "balanceDeltaOrg trebuie să fie NEUTRAL (0.5) în context live");
    }

    @Test
    void build_balanceDeltaDest_isNeutral() {
        // Balanțele nu sunt disponibile în context live → NEUTRAL = 0.5
        FraudEvaluationRequest req = buildRequest(1000.0, 60, null);
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(FraudFeatureEngine.NEUTRAL, vector[2], 0.001,
                "balanceDeltaDest trebuie să fie NEUTRAL (0.5) în context live");
    }

    @Test
    void build_typeRisk_isHighForTransferExternal() {
        FraudEvaluationRequest req = buildRequest(500.0, 60, "TRANSFER_EXTERNAL");
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(1.0, vector[3], 0.001, "TRANSFER_EXTERNAL → typeRisk = 1.0");
    }

    @Test
    void build_typeRisk_isZeroForDeposit() {
        FraudEvaluationRequest req = buildRequest(500.0, 60, "DEPOSIT");
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(0.0, vector[3], 0.001, "DEPOSIT → typeRisk = 0.0");
    }

    @Test
    void build_typeRisk_isNeutralForUnknownType() {
        FraudEvaluationRequest req = buildRequest(500.0, 60, "UNKNOWN_TYPE");
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(FraudFeatureEngine.NEUTRAL, vector[3], 0.001,
                "Tip necunoscut → typeRisk = NEUTRAL (0.5)");
    }

    @Test
    void build_hourSuspicion_isBinaryValue() {
        // Nu putem controla ceasul serverului în test, dar verificăm că e binar
        FraudEvaluationRequest req = buildRequest(500.0, 60, null);
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertTrue(vector[4] == 0.0 || vector[4] == 1.0,
                "hourSuspicion trebuie să fie binar: 0.0 sau 1.0");
    }

    @Test
    void build_newAccountFlag_isOneForNewAccount() {
        // Cont cu vârsta 10 zile < 30 → flag = 1.0
        FraudEvaluationRequest req = buildRequest(500.0, 10, null);
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(1.0, vector[5], 0.001, "Cont de 10 zile → newAccountFlag = 1.0");
    }

    @Test
    void build_newAccountFlag_isZeroForOldAccount() {
        // Cont cu vârsta 365 zile → flag = 0.0
        FraudEvaluationRequest req = buildRequest(500.0, 365, null);
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(0.0, vector[5], 0.001, "Cont de 365 zile → newAccountFlag = 0.0");
    }

    @Test
    void build_allFeaturesInValidRange() {
        FraudEvaluationRequest req = buildRequest(2500.0, 45, "WITHDRAWAL");
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        for (int i = 0; i < vector.length; i++) {
            assertTrue(vector[i] >= 0.0 && vector[i] <= 1.0,
                    "Feature[" + i + "] = " + vector[i] + " trebuie să fie în [0,1]");
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static FraudEvaluationRequest buildRequest(double amount, int ageDays, String type) {
        FraudEvaluationRequest req = new FraudEvaluationRequest();
        req.setAmount(amount);
        req.setAccountAgeDays(ageDays);
        req.setTransactionType(type);
        return req;
    }

    private static ScoringResult emptyScoringResult() {
        return new ScoringResult(0.0, Collections.emptyMap(), "test");
    }
}
