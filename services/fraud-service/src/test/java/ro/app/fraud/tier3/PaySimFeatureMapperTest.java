package ro.app.fraud.tier3;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Teste unitare pentru PaySimFeatureMapper — verifică mapping-ul PaySim →
 * vector de features.
 * Validează consistența între antrenament (PaySim) și inferență (live PSD2).
 */
class PaySimFeatureMapperTest {

    @Test
    void fromPaySim_produces6DimensionalVector() {
        PaySimRow row = new PaySimRow(10, "TRANSFER", 1000.0, 5000.0, 4000.0, 0.0, 1000.0, 0);
        double[] features = PaySimFeatureMapper.fromPaySim(row);
        assertEquals(6, features.length, "Vectorul trebuie să aibă exact 6 dimensiuni");
    }

    @Test
    void fromPaySim_amountRatio_normalizedCorrectly() {
        // 25000 / 50000 = 0.5
        PaySimRow row = new PaySimRow(10, "PAYMENT", 25_000.0, 50_000.0, 25_000.0, 0.0, 25_000.0, 0);
        double[] features = PaySimFeatureMapper.fromPaySim(row);
        assertEquals(0.5, features[0], 1e-9, "amountRatio: 25000/50000 = 0.5");
    }

    @Test
    void fromPaySim_typeRisk_transferIsFraudRisk() {
        PaySimRow row = new PaySimRow(10, "TRANSFER", 1000.0, 5000.0, 4000.0, 0.0, 1000.0, 0);
        double[] features = PaySimFeatureMapper.fromPaySim(row);
        assertEquals(3.0, features[1], 1e-9, "TRANSFER → typeRisk = 3.0");
    }

    @Test
    void fromPaySim_typeRisk_cashOutIsFraudRisk() {
        PaySimRow row = new PaySimRow(10, "CASH_OUT", 1000.0, 5000.0, 4000.0, 0.0, 0.0, 0);
        double[] features = PaySimFeatureMapper.fromPaySim(row);
        assertEquals(3.0, features[1], 1e-9, "CASH_OUT → typeRisk = 3.0");
    }

    @Test
    void fromPaySim_typeRisk_cashInIsLowRisk() {
        PaySimRow row = new PaySimRow(10, "CASH_IN", 1000.0, 5000.0, 6000.0, 10000.0, 9000.0, 0);
        double[] features = PaySimFeatureMapper.fromPaySim(row);
        assertEquals(0.0, features[1], 1e-9, "CASH_IN → typeRisk = 0.0");
    }

    @Test
    void fromPaySim_hourSuspicion_nighttimeStep() {
        // step=3 → step % 24 = 3 → ora 3 noaptea → risc maxim (3.0)
        PaySimRow row = new PaySimRow(3, "PAYMENT", 100.0, 1000.0, 900.0, 0.0, 100.0, 0);
        double[] features = PaySimFeatureMapper.fromPaySim(row);
        assertEquals(3.0, features[2], 1e-9, "step=3 (ora 3 noaptea) → hourSuspicion = 3.0");
    }

    @Test
    void fromPaySim_hourSuspicion_daytimeStep() {
        // step=12 → step % 24 = 12 → ora 12 ziua → risc normal (1.0)
        PaySimRow row = new PaySimRow(12, "PAYMENT", 100.0, 1000.0, 900.0, 0.0, 100.0, 0);
        double[] features = PaySimFeatureMapper.fromPaySim(row);
        assertEquals(1.0, features[2], 1e-9, "step=12 (ora 12 ziua) → hourSuspicion = 1.0");
    }

    @Test
    void fromPaySim_newAccountFlag_earlyStep_isNew() {
        // FIX #1b: step < 24 → newAccountFlag = 1.0
        PaySimRow row = new PaySimRow(10, "TRANSFER", 1000.0, 5000.0, 4000.0, 0.0, 1000.0, 1);
        double[] features = PaySimFeatureMapper.fromPaySim(row);
        assertEquals(1.0, features[3], 1e-9, "step=10 (< 24) → cont 'nou' = 1.0");
    }

    @Test
    void fromPaySim_newAccountFlag_lateStep_isOld() {
        // step >= 24 → newAccountFlag = 0.0
        PaySimRow row = new PaySimRow(100, "TRANSFER", 1000.0, 5000.0, 4000.0, 0.0, 1000.0, 0);
        double[] features = PaySimFeatureMapper.fromPaySim(row);
        assertEquals(0.0, features[3], 1e-9, "step=100 (≥ 24) → cont 'vechi' = 0.0");
    }

    @Test
    void fromPaySim_senderDepletion_fullDrain() {
        // amount == oldBalance → depletion = 1.0 (ATO signature)
        PaySimRow row = new PaySimRow(10, "CASH_OUT", 5000.0, 5000.0, 0.0, 0.0, 5000.0, 1);
        double[] features = PaySimFeatureMapper.fromPaySim(row);
        assertEquals(1.0, features[4], 1e-9, "Golire completă cont → senderDepletionRatio = 1.0");
    }

    @Test
    void fromPaySim_senderDepletion_partialDrain() {
        // 1000 / 5000 = 0.2
        PaySimRow row = new PaySimRow(10, "PAYMENT", 1000.0, 5000.0, 4000.0, 0.0, 1000.0, 0);
        double[] features = PaySimFeatureMapper.fromPaySim(row);
        assertEquals(0.2, features[4], 1e-9, "senderDepletionRatio: 1000/5000 = 0.2");
    }

    @Test
    void fromPaySim_roundAmount_detected() {
        PaySimRow row = new PaySimRow(10, "PAYMENT", 500.0, 5000.0, 4500.0, 0.0, 500.0, 0);
        double[] features = PaySimFeatureMapper.fromPaySim(row);
        assertEquals(1.0, features[5], 1e-9, "500.0 = sumă rotundă");
    }

    @Test
    void fromPaySim_nonRoundAmount() {
        PaySimRow row = new PaySimRow(10, "PAYMENT", 347.89, 5000.0, 4652.11, 0.0, 347.89, 0);
        double[] features = PaySimFeatureMapper.fromPaySim(row);
        assertEquals(0.0, features[5], 1e-9, "347.89 = sumă nerotundă");
    }

    // ── Consistency test: PaySim și Live produc aceleași valori ──────────────

    @Test
    void consistency_paySimTransferAndLiveExternal_sameTypeRisk() {
        // PaySim TRANSFER (antrenament) = Live TRANSFER_EXTERNAL (inferentă) = 3.0
        double paySimRisk = FraudFeatureEngine.computeTypeRiskPaySim("TRANSFER");
        double liveRisk = FraudFeatureEngine.computeTypeRiskLive("TRANSFER_EXTERNAL");
        assertEquals(paySimRisk, liveRisk, 1e-9,
                "PaySim TRANSFER și Live TRANSFER_EXTERNAL trebuie să producă aceeași valoare (3.0)");
    }

    @Test
    void consistency_paySimCashInAndLivePosPayment_sameTypeRisk() {
        double paySimRisk = FraudFeatureEngine.computeTypeRiskPaySim("CASH_IN");
        double liveRisk = FraudFeatureEngine.computeTypeRiskLive("POS_PAYMENT");
        assertEquals(paySimRisk, liveRisk, 1e-9,
                "PaySim CASH_IN și Live POS_PAYMENT trebuie să producă aceeași valoare (0.0)");
    }
}
