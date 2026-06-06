package ro.app.fraud.tier3;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Teste unitare pentru PaySimFeatureMapper (JUnit 4).
 *
 * Verifica mapping-ul PaySim → vector de features si consistenta
 * intre antrenament (PaySim) si inferenta (live PSD2).
 *
 * Pattern testat: Builder (Creational) — construirea vectorului de features
 * din date PaySim brute.
 */
@RunWith(JUnit4.class)
public class PaySimFeatureMapperTest {

    @Test
    public void fromPaySim_produces6DimensionalVector() {
        PaySimRow row = new PaySimRow(10, "TRANSFER", 1000.0, 5000.0, 4000.0, 0.0, 1000.0, 0);

        double[] features = PaySimFeatureMapper.fromPaySim(row);

        assertEquals("Vector must have 6 dimensions", 6, features.length);
    }

    @Test
    public void fromPaySim_amountRatio_normalizedCorrectly() {
        // 25000 / 50000 = 0.5
        PaySimRow row = new PaySimRow(10, "PAYMENT", 25_000.0, 50_000.0, 25_000.0, 0.0, 25_000.0, 0);

        double[] features = PaySimFeatureMapper.fromPaySim(row);

        assertEquals("amountRatio: 25000/50000 = 0.5", 0.5, features[0], 1e-9);
    }

    @Test
    public void fromPaySim_typeRisk_transferIsFraudRisk() {
        PaySimRow row = new PaySimRow(10, "TRANSFER", 1000.0, 5000.0, 4000.0, 0.0, 1000.0, 0);

        double[] features = PaySimFeatureMapper.fromPaySim(row);

        assertEquals("TRANSFER → typeRisk = 3.0", 3.0, features[1], 1e-9);
    }

    @Test
    public void fromPaySim_typeRisk_cashOutIsFraudRisk() {
        PaySimRow row = new PaySimRow(10, "CASH_OUT", 1000.0, 5000.0, 4000.0, 0.0, 0.0, 0);

        double[] features = PaySimFeatureMapper.fromPaySim(row);

        assertEquals("CASH_OUT → typeRisk = 3.0", 3.0, features[1], 1e-9);
    }

    @Test
    public void fromPaySim_typeRisk_cashInIsLowRisk() {
        PaySimRow row = new PaySimRow(10, "CASH_IN", 1000.0, 5000.0, 6000.0, 10000.0, 9000.0, 0);

        double[] features = PaySimFeatureMapper.fromPaySim(row);

        assertEquals("CASH_IN → typeRisk = 0.0", 0.0, features[1], 1e-9);
    }

    @Test
    public void fromPaySim_hourSuspicion_nighttimeStep_isMaxRisk() {
        // step=3 → step % 24 = 3 → ora 3 noaptea → risc maxim (3.0)
        PaySimRow row = new PaySimRow(3, "PAYMENT", 100.0, 1000.0, 900.0, 0.0, 100.0, 0);

        double[] features = PaySimFeatureMapper.fromPaySim(row);

        assertEquals("step=3 (3 AM) → hourSuspicion = 3.0", 3.0, features[2], 1e-9);
    }

    @Test
    public void fromPaySim_hourSuspicion_daytimeStep_isNormalRisk() {
        // step=12 → step % 24 = 12 → ora 12 ziua → risc normal (1.0)
        PaySimRow row = new PaySimRow(12, "PAYMENT", 100.0, 1000.0, 900.0, 0.0, 100.0, 0);

        double[] features = PaySimFeatureMapper.fromPaySim(row);

        assertEquals("step=12 (12 PM) → hourSuspicion = 1.0", 1.0, features[2], 1e-9);
    }

    @Test
    public void fromPaySim_newAccountFlag_earlyStep_isNew() {
        PaySimRow row = new PaySimRow(10, "TRANSFER", 1000.0, 5000.0, 4000.0, 0.0, 1000.0, 1);

        double[] features = PaySimFeatureMapper.fromPaySim(row);

        assertEquals("step=10 (< 24) → 'new' account = 1.0", 1.0, features[3], 1e-9);
    }

    @Test
    public void fromPaySim_newAccountFlag_lateStep_isOld() {
        // step >= 24 → newAccountFlag = 0.0
        PaySimRow row = new PaySimRow(100, "TRANSFER", 1000.0, 5000.0, 4000.0, 0.0, 1000.0, 0);

        double[] features = PaySimFeatureMapper.fromPaySim(row);

        assertEquals("step=100 (>= 24) → 'old' account = 0.0", 0.0, features[3], 1e-9);
    }

    @Test
    public void fromPaySim_senderDepletion_fullDrain_isOne() {
        // amount == oldBalance → depletion = 1.0 (ATO pattern)
        PaySimRow row = new PaySimRow(10, "CASH_OUT", 5000.0, 5000.0, 0.0, 0.0, 5000.0, 1);

        double[] features = PaySimFeatureMapper.fromPaySim(row);

        assertEquals("Complete drain → senderDepletionRatio = 1.0", 1.0, features[4], 1e-9);
    }

    @Test
    public void fromPaySim_senderDepletion_partialDrain_calculatedCorrectly() {
        // 1000 / 5000 = 0.2
        PaySimRow row = new PaySimRow(10, "PAYMENT", 1000.0, 5000.0, 4000.0, 0.0, 1000.0, 0);

        double[] features = PaySimFeatureMapper.fromPaySim(row);

        assertEquals("senderDepletionRatio: 1000/5000 = 0.2", 0.2, features[4], 1e-9);
    }

    @Test
    public void fromPaySim_roundAmount_detected() {
        PaySimRow row = new PaySimRow(10, "PAYMENT", 500.0, 5000.0, 4500.0, 0.0, 500.0, 0);

        double[] features = PaySimFeatureMapper.fromPaySim(row);

        assertEquals("500.0 → round amount = 1.0", 1.0, features[5], 1e-9);
    }

    @Test
    public void fromPaySim_nonRoundAmount_isZero() {
        PaySimRow row = new PaySimRow(10, "PAYMENT", 347.89, 5000.0, 4652.11, 0.0, 347.89, 0);

        double[] features = PaySimFeatureMapper.fromPaySim(row);

        assertEquals("347.89 → non-round amount = 0.0", 0.0, features[5], 1e-9);
    }

    // ── Teste de consistenta PaySim ↔ Live ────────────────────────────────────

    @Test
    public void consistency_paySimTransferAndLiveExternal_sameTypeRisk() {
        // PaySim TRANSFER = Live TRANSFER_EXTERNAL = 3.0
        double paySimRisk = FraudFeatureEngine.computeTypeRiskPaySim("TRANSFER");
        double liveRisk = FraudFeatureEngine.computeTypeRiskLive("TRANSFER_EXTERNAL");

        assertEquals("PaySim TRANSFER and Live TRANSFER_EXTERNAL must produce 3.0",
                paySimRisk, liveRisk, 1e-9);
    }

    @Test
    public void consistency_paySimCashInAndLivePosPayment_sameTypeRisk() {
        double paySimRisk = FraudFeatureEngine.computeTypeRiskPaySim("CASH_IN");
        double liveRisk = FraudFeatureEngine.computeTypeRiskLive("POS_PAYMENT");

        assertEquals("PaySim CASH_IN and Live POS_PAYMENT must produce 0.0",
                paySimRisk, liveRisk, 1e-9);
    }
}
