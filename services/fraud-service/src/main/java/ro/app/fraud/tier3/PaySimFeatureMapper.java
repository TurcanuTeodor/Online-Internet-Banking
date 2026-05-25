package ro.app.fraud.tier3;

/**
 * =====================================================================
 * — Adaptor de Antrenament (PaySim → Vector PSD2)
 * =====================================================================
 *
 * ROL:
 *     1. Extrage valorile brute din campurile PaySimRow
 *     2. Le paseaza metodelor statice din FraudFeatureEngine
 *     3. Asambleaza vectorul final de 6 dimensiuni IDENTIC cu FeatureVectorBuilder
 *
 * STRATEGIE: Open Payments / PSD2 — vector comportamental, fara balanta destinatar.
 * Vectorul: [amountRatio, typeRisk, hourSuspicion, newAccountFlag, senderDepletionRatio, isRoundAmount]
 */
public final class PaySimFeatureMapper {

    private PaySimFeatureMapper() {
        // utils class
    }

    public static double[] fromPaySim(PaySimRow row) {
        return new double[]{
            // [0] amountRatio — suma normalizata la plafonul legal Transfond (50.000 RON)
            FraudFeatureEngine.computeAmountRatio(row.amount(), FraudFeatureEngine.LEGAL_AMOUNT_CAP),

            // [1] typeRisk — mapeaza tipurile PaySim pe scara PSD2 (TRANSFER/CASH_OUT = 3.0)
            FraudFeatureEngine.computeTypeRiskPaySim(row.type()),

            // [2] hourSuspicion — step PaySim simulat ca ora [0, 23] cu grupe de risc ciclice
            FraudFeatureEngine.computeHourSuspicionFromClock(row.step() % 24),

            // [3] newAccountFlag — PaySim nu are varsta contului, se lasa 0.0 la antrenament
            0.0,

            // [4] senderDepletionRatio — procentul din contul senderului golit (ATO signature)
            FraudFeatureEngine.computeSenderDepletionRatio(row.amount(), row.oldbalanceOrg()),

            // [5] isRoundAmount — flag suma rotunda (specific atacurilor Cash-Out)
            FraudFeatureEngine.computeRoundAmountFlag(row.amount())
        };
    }
}
