package ro.app.fraud.tier3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import ro.app.fraud.dto.FraudEvaluationRequest;
import ro.app.fraud.tier2.ScoringResult;

/**
 * Teste pentru FeatureVectorBuilder (după refactorizare cu strategia PSD2).
 *
 * NOTA: Testele verifica vectorul RAW (fara MinMaxScaler) via metoda @Deprecated
 * build(req, scoring) — aceasta este intentionat: testele de unit ar trebui sa
 * verifice feature engineering-ul, nu scaling-ul (care depinde de date de antrenament).
 *
 * Testăm că:
 *   1. Vectorul are exact 6 dimensiuni
 *   2. Fiecare feature este calculat corect conform strategiei PSD2:
 *      [amountRatio, typeRisk, hourSuspicion, newAccountFlag, senderDepletionRatio, isRoundAmount]
 *   3. Fix #1a: TRANSFER_EXTERNAL mapat pe 3.0 (nu 2.0)
 *   4. Fix #10: isRoundAmount cu epsilon floating-point
 */
@SuppressWarnings("deprecation")  // Intentionat: testam feature engineering pur, fara scaling
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

    // FIX #1a: TRANSFER_EXTERNAL trebuie sa returneze 3.0 (nu 2.0 ca inainte)
    @Test
    void build_typeRisk_isHighForTransferExternal_fix1a() {
        FraudEvaluationRequest req = buildRequest(500.0, 60, "TRANSFER_EXTERNAL", 5000.0);
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(3.0, vector[1], 0.001,
            "FIX #1a: TRANSFER_EXTERNAL → typeRisk = 3.0 (aliniat cu PaySim, nu 2.0 out-of-distribution)");
    }

    @Test
    void build_typeRisk_isZeroForPosPayment() {
        FraudEvaluationRequest req = buildRequest(500.0, 60, "POS_PAYMENT", 5000.0);
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(0.0, vector[1], 0.001, "POS_PAYMENT → typeRisk = 0.0");
    }

    @Test
    void build_typeRisk_isOneForTransferInternal() {
        FraudEvaluationRequest req = buildRequest(500.0, 60, "TRANSFER_INTERNAL", 5000.0);
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(1.0, vector[1], 0.001, "TRANSFER_INTERNAL → typeRisk = 1.0");
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
    void build_hourSuspicion_usesTransactionHour_whenProvided() {
        // FIX #14: daca transactionHour este setat, trebuie sa il foloseasca
        FraudEvaluationRequest req = buildRequest(500.0, 60, "TRANSFER_INTERNAL", 5000.0);
        req.setTransactionHour(3); // ora 3 noaptea = risc maxim 3.0

        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(3.0, vector[2], 0.001,
            "FIX #14: transactionHour=3 (noapte) → hourSuspicion = 3.0");
    }

    @Test
    void build_hourSuspicion_fallsBackToServerTime_whenNotProvided() {
        // transactionHour=-1 (default) → fallback la ora serverului
        FraudEvaluationRequest req = buildRequest(500.0, 60, "TRANSFER_INTERNAL", 5000.0);
        // transactionHour ramanele -1 (default)

        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertTrue(vector[2] >= 1.0 && vector[2] <= 3.0,
            "FIX #14: fallback la ora serverului → hourSuspicion in range valid");
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

    // FIX #10: Test epsilon floating-point pentru sume cu erori de conversie
    @Test
    void build_isRoundAmount_handlesFloatingPointErrors_fix10() {
        // Simuleaza o eroare de conversie valutara minima
        FraudEvaluationRequest req = buildRequest(500.0000000001, 60, "TRANSFER_INTERNAL", 5000.0);
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(1.0, vector[5], 0.001,
            "FIX #10: 500.0000000001 trebuie detectat ca suma rotunda (epsilon tolerance)");
    }

    @Test
    void build_isRoundAmount_isZeroForObviouslyNonRound() {
        FraudEvaluationRequest req = buildRequest(537.50, 60, "TRANSFER_INTERNAL", 5000.0);
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult());

        assertEquals(0.0, vector[5], 0.001, "537.50 nu este suma rotunda");
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
