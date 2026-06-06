package ro.app.fraud.tier3;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import ro.app.fraud.dto.FraudEvaluationRequest;
import ro.app.fraud.tier2.ScoringResult;
import smile.anomaly.IsolationForest;

/**
 * Teste unitare pentru FeatureVectorBuilder (JUnit 4).
 *
 * Testeaza vectorul RAW (fara MinMaxScaler) prin metoda @Deprecated build(req,
 * scoring).
 * Scopul este verificarea feature engineering-ului, nu a scaling-ului.
 *
 * @see FraudFeatureEngine pentru testele individuale per feature.
 */
@SuppressWarnings("deprecation")
@RunWith(JUnit4.class)
public class FeatureVectorBuilderTest {

    @Test
    public void build_producesSixDimensionalVector() {
        // Arrange
        FraudEvaluationRequest req = new FraudEvaluationRequest();
        req.setAmount(1000.0);
        req.setAccountAgeDays(60);
        req.setTransactionType("TRANSFER_EXTERNAL");
        req.setOldBalanceOrg(5000.0);

        // Act
        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult(), defaultSnapshot());

        // Assert
        assertEquals("Vector must have exactly 6 dimensions", 6, vector.length);
    }

    @Test
    public void build_amountRatio_normalizedCorrectly() {
        // 1000 / 50000 = 0.02
        FraudEvaluationRequest req = buildRequest(1000.0, 60, "TRANSFER_EXTERNAL", 5000.0);

        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult(), defaultSnapshot());

        assertEquals("amountRatio: 1000/50000 = 0.02", 0.02, vector[0], 0.001);
    }

    @Test
    public void build_amountRatio_cappedAtOne() {
        // 60000 / 50000 = 1.2 → capped la 1.0
        FraudEvaluationRequest req = buildRequest(60_000.0, 60, "TRANSFER_EXTERNAL", 100_000.0);

        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult(), defaultSnapshot());

        assertEquals("amountRatio capped at 1.0", 1.0, vector[0], 0.001);
    }

    @Test
    public void build_typeRisk_transferInstant_isHighRisk() {
        FraudEvaluationRequest req = buildRequest(500.0, 60, "TRANSFER_INSTANT", 5000.0);

        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult(), defaultSnapshot());

        assertEquals("TRANSFER_INSTANT → typeRisk scaled = 1.0", 1.0, vector[1], 0.001);
    }

    @Test
    public void build_typeRisk_transferExternal_fix1a() {
        FraudEvaluationRequest req = buildRequest(500.0, 60, "TRANSFER_EXTERNAL", 5000.0);

        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult(), defaultSnapshot());

        assertEquals("FIX #1a: TRANSFER_EXTERNAL → typeRisk scaled = 1.0", 1.0, vector[1], 0.001);
    }

    @Test
    public void build_typeRisk_posPayment_isZero() {
        FraudEvaluationRequest req = buildRequest(500.0, 60, "POS_PAYMENT", 5000.0);

        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult(), defaultSnapshot());

        assertEquals("POS_PAYMENT → typeRisk = 0.0", 0.0, vector[1], 0.001);
    }

    @Test
    public void build_hourSuspicion_isInValidRange() {
        FraudEvaluationRequest req = buildRequest(500.0, 60, "TRANSFER_INTERNAL", 5000.0);

        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult(), defaultSnapshot());

        assertTrue("hourSuspicion scaled must be in [0.0, 1.0]",
                vector[2] >= 0.0 && vector[2] <= 1.0);
    }

    @Test
    public void build_hourSuspicion_nighttimeHour_isMaxRisk() {
        // FIX #14: transactionHour=3 (noapte) → hourSuspicion = 1.0
        FraudEvaluationRequest req = buildRequest(500.0, 60, "TRANSFER_INTERNAL", 5000.0);
        req.setTransactionHour(3);

        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult(), defaultSnapshot());

        assertEquals("FIX #14: 3 AM → hourSuspicion scaled = 1.0", 1.0, vector[2], 0.001);
    }

    @Test
    public void build_newAccountFlag_newAccount_isOne() {
        // Cont de 10 zile < 30 → flag = 1.0
        FraudEvaluationRequest req = buildRequest(500.0, 10, "TRANSFER_INTERNAL", 5000.0);

        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult(), defaultSnapshot());

        assertEquals("New account (10 days) → newAccountFlag = 1.0", 1.0, vector[3], 0.001);
    }

    @Test
    public void build_newAccountFlag_oldAccount_isZero() {
        // Cont de 365 zile → flag = 0.0
        FraudEvaluationRequest req = buildRequest(500.0, 365, "TRANSFER_INTERNAL", 5000.0);

        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult(), defaultSnapshot());

        assertEquals("Old account (365 days) → newAccountFlag = 0.0", 0.0, vector[3], 0.001);
    }

    @Test
    public void build_senderDepletionRatio_calculatedCorrectly() {
        // 500 / 2000 = 0.25
        FraudEvaluationRequest req = buildRequest(500.0, 60, "TRANSFER_INTERNAL", 2000.0);

        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult(), defaultSnapshot());

        assertEquals("senderDepletionRatio: 500/2000 = 0.25", 0.25, vector[4], 0.001);
    }

    @Test
    public void build_senderDepletionRatio_nullBalance_isZero() {
        FraudEvaluationRequest req = buildRequest(500.0, 60, "TRANSFER_INTERNAL", null);

        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult(), defaultSnapshot());

        assertEquals("null oldBalanceOrg → senderDepletionRatio = 0.0", 0.0, vector[4], 0.001);
    }

    @Test
    public void build_isRoundAmount_roundNumber_isOne() {
        FraudEvaluationRequest req = buildRequest(500.0, 60, "TRANSFER_INTERNAL", 5000.0);

        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult(), defaultSnapshot());

        assertEquals("500.0 → isRoundAmount = 1.0", 1.0, vector[5], 0.001);
    }

    @Test
    public void build_isRoundAmount_nonRoundNumber_isZero() {
        FraudEvaluationRequest req = buildRequest(537.5, 60, "TRANSFER_INTERNAL", 5000.0);

        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult(), defaultSnapshot());

        assertEquals("537.5 → isRoundAmount = 0.0", 0.0, vector[5], 0.001);
    }

    @Test
    public void build_isRoundAmount_floatingPointEpsilon_fix10() {
        FraudEvaluationRequest req = buildRequest(500.0000000001, 60, "TRANSFER_INTERNAL", 5000.0);

        double[] vector = FeatureVectorBuilder.build(req, emptyScoringResult(), defaultSnapshot());

        assertEquals("FIX #10: 500.0000000001 → round = 1.0", 1.0, vector[5], 0.001);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static FraudEvaluationRequest buildRequest(double amount, int ageDays,
            String type, Double oldBalance) {
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

    private static ModelStore.ModelSnapshot defaultSnapshot() {
        double[][] dummyData = {
                { 0.1, 0.2, 1.0, 0.0, 0.0, 0.0 },
                { 0.2, 0.3, 2.0, 0.0, 0.0, 0.0 }
        };
        IsolationForest dummyModel = IsolationForest.fit(dummyData, 10, 5, 0.1, 0);

        double[] means = { 0.15, 0.25, 1.5, 0.0, 0.0, 0.0 };
        double[] mins = { 0.0, 0.0, 1.0, 0.0, 0.0, 0.0 };
        double[] maxes = { 1.0, 3.0, 3.0, 1.0, 1.0, 1.0 };

        return new ModelStore.ModelSnapshot(
                dummyModel, 0.5, means, mins, maxes,
                ModelStore.currentVersion(), 0.0, 1);
    }
}
