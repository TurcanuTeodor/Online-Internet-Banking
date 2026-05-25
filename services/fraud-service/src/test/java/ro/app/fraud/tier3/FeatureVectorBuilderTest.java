package ro.app.fraud.tier3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import ro.app.fraud.dto.FraudEvaluationRequest;
import ro.app.fraud.tier2.ScoringResult;

/**
 * Teste pentru FeatureVectorBuilder (după refactorizare cu strategia PSD2).
 *
 * Testăm că:
 *   1. Vectorul are exact 6 dimensiuni
 *   2. Fiecare feature este calculat corect conform noii strategii PSD2:
 *      [amountRatio, typeRisk, hourSuspicion, newAccountFlag, senderDepletionRatio, isRoundAmount]
 */
class FeatureVectorBuilderTest {

    @Test
    void build_producesSixDimensionalVector() {
        FraudEvaluationRequest req = new FraudEvaluationRequest();
        req.setAmount(1000.0);
        req.setAccountAgeDays(60);
        req.setTransactionType("TRANSFER_EXTERNAL");
        req.setOldBalanceOrg(5000.0);

        ScoringResult scoring = emptyScoringResult();
        double[] vector = FeatureVectorBuilder.build(req, scoring);

        assertEquals(6, vector.length, "Vectorul trebuie să aibă exact 6 dimensiuni");
    }

    @Test
    void build_amountRatio_isNormalizedWithLiveCap() {
        // 1000 / 50000 = 0.02
        FraudEvaluationRequest req = buildRequest(1000.0, 60, "TRANSFER_EXTERNAL", 5000.0);
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(0.02, vector[0], 0.001, "amountRatio: 1000/50000 = 0.02");
    }

    @Test
    void build_amountRatio_isCappedAtOne() {
        // 60000 / 50000 = 1.2 → capped la 1.0
        FraudEvaluationRequest req = buildRequest(60_000.0, 60, "TRANSFER_EXTERNAL", 100_000.0);
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(1.0, vector[0], 0.001, "amountRatio: cap la 1.0 pentru sume mari");
    }

    @Test
    void build_typeRisk_isHighForTransferInstant() {
        FraudEvaluationRequest req = buildRequest(500.0, 60, "TRANSFER_INSTANT", 5000.0);
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(3.0, vector[1], 0.001, "TRANSFER_INSTANT → typeRisk = 3.0");
    }

    @Test
    void build_typeRisk_isZeroForPosPayment() {
        FraudEvaluationRequest req = buildRequest(500.0, 60, "POS_PAYMENT", 5000.0);
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(0.0, vector[1], 0.001, "POS_PAYMENT → typeRisk = 0.0");
    }

    @Test
    void build_typeRisk_isDefaultForUnknownType() {
        FraudEvaluationRequest req = buildRequest(500.0, 60, "UNKNOWN_TYPE", 5000.0);
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(1.0, vector[1], 0.001, "Tip necunoscut → typeRisk = 1.0");
    }

    @Test
    void build_hourSuspicion_isInValidRange() {
        FraudEvaluationRequest req = buildRequest(500.0, 60, "TRANSFER_INTERNAL", 5000.0);
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertTrue(vector[2] >= 1.0 && vector[2] <= 3.0,
                "hourSuspicion trebuie să fie 1.0, 2.0 sau 3.0");
    }

    @Test
    void build_newAccountFlag_isOneForNewAccount() {
        // Cont cu vârsta 10 zile < 30 → flag = 1.0
        FraudEvaluationRequest req = buildRequest(500.0, 10, "TRANSFER_INTERNAL", 5000.0);
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(1.0, vector[3], 0.001, "Cont de 10 zile → newAccountFlag = 1.0");
    }

    @Test
    void build_newAccountFlag_isZeroForOldAccount() {
        // Cont cu vârsta 365 zile → flag = 0.0
        FraudEvaluationRequest req = buildRequest(500.0, 365, "TRANSFER_INTERNAL", 5000.0);
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(0.0, vector[3], 0.001, "Cont de 365 zile → newAccountFlag = 0.0");
    }

    @Test
    void build_senderDepletionRatio_isCalculatedCorrectly() {
        // 500 / 2000 = 0.25
        FraudEvaluationRequest req = buildRequest(500.0, 60, "TRANSFER_INTERNAL", 2000.0);
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(0.25, vector[4], 0.001, "senderDepletionRatio: 500/2000 = 0.25");
    }

    @Test
    void build_senderDepletionRatio_isZeroIfBalanceUnknown() {
        FraudEvaluationRequest req = buildRequest(500.0, 60, "TRANSFER_INTERNAL", null);
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(0.0, vector[4], 0.001, "senderDepletionRatio = 0.0 când oldBalanceOrg e null");
    }

    @Test
    void build_isRoundAmount_isOneForRoundNumbers() {
        FraudEvaluationRequest req = buildRequest(500.0, 60, "TRANSFER_INTERNAL", 5000.0);
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(1.0, vector[5], 0.001, "Suma rotunda (500) → isRoundAmount = 1.0");
    }

    @Test
    void build_isRoundAmount_isZeroForNonRoundNumbers() {
        FraudEvaluationRequest req = buildRequest(537.5, 60, "TRANSFER_INTERNAL", 5000.0);
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(0.0, vector[5], 0.001, "Suma nerotunda (537.5) → isRoundAmount = 0.0");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static FraudEvaluationRequest buildRequest(double amount, int ageDays, String type, Double oldBalance) {
        FraudEvaluationRequest req = new FraudEvaluationRequest();
        req.setAmount(amount);
        req.setAccountAgeDays(ageDays);
        req.setTransactionType(type);
        req.setOldBalanceOrg(oldBalance);
        return req;
    }

    private static ScoringResult emptyScoringResult() {
        return new ScoringResult(0.0, Collections.emptyMap(), "test");
    }
}
