package ro.app.fraud.tier3;

/**
 * =====================================================================
 * — Adaptor de Antrenament 
 * =====================================================================
 *
* ROL: 
*     1. Extrage valorile brute din campurile PaySimRow
*     2. Le paseaza metodelor statice din FraudFeatureEngine
*     3. Asambleaza vectorul final de 6 dimensiuni
 */
public final class PaySimFeatureMapper {

    private PaySimFeatureMapper() {
        // utils class
    }

    public static double[] fromPaySim(PaySimRow row) {
        return new double[]{
            // [0] Normalizare suma cu plafonul specific PaySim (10.000)
            FraudFeatureEngine.computeAmountRatio(row.amount(), FraudFeatureEngine.PAYSIM_AMOUNT_CAP),

            // [1] Fractie balanta iesita din sender (0=nimic, 1=golit complet)
            FraudFeatureEngine.computeBalanceDeltaOrg(row.oldbalanceOrg(), row.newbalanceOrig()),

            // [2] Fractie suma primita de destinatar fata de astepari
            FraudFeatureEngine.computeBalanceDeltaDest(row.oldbalanceDest(), row.newbalanceDest(), row.amount()),

            // [3] Risc bazat pe tipul PaySim (TRANSFER/CASH_OUT = 1.0)
            FraudFeatureEngine.computeTypeRiskPaySim(row.type()),

            // [4] Ora noaptea simulata: step % 24 ∈ [0, 6) → 1.0
            FraudFeatureEngine.computeHourSuspicionFromStep(row.step()),

            // [5] Cont cu balanta zero initiinda tranzactie = anomalie PaySim
            FraudFeatureEngine.computeNewAccountFlagFromBalance(row.oldbalanceOrg(), row.amount())
        };
    }
}
