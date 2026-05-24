package ro.app.fraud.tier3;

import java.util.Arrays;

/**
 * Construiește textul de explicație al verdictului ML pentru utilizator/admin.
 *
 * Etichetele din FEATURE_NAMES sunt mapate 1:1 cu ordinea vectorului produs
 * de FraudFeatureEngine și asamblat de PaySimFeatureMapper / FeatureVectorBuilder:
 *
 *   [0] amountRatio       → "unusually large transaction amount"
 *   [1] balanceDeltaOrg   → "sender account balance fully drained"
 *   [2] balanceDeltaDest  → "recipient balance mismatch after transfer"
 *   [3] typeRisk          → "high-risk transaction type (external/withdrawal)"
 *   [4] hourSuspicion     → "transaction during suspicious night-time hours"
 *   [5] newAccountFlag    → "new or recently opened account"
 *
 * IMPORTANT: Dacă ordinea vectorului se schimbă în FraudFeatureEngine,
 * actualizează și acest array în consecință.
 */
public final class ReasoningBuilder {

    /**
     * Etichete human-readable aliniate 1:1 cu vectorul de 6 features.
     * Ordinea trebuie să corespundă exact cu FraudFeatureEngine.
     */
    public static final String[] FEATURE_NAMES = {
        "unusually large transaction amount",                // [0] amountRatio
        "sender account balance fully drained",              // [1] balanceDeltaOrg
        "recipient balance mismatch after transfer",         // [2] balanceDeltaDest
        "high-risk transaction type (external/withdrawal)",  // [3] typeRisk
        "transaction during suspicious night-time hours",    // [4] hourSuspicion
        "new or recently opened account"                     // [5] newAccountFlag
    };

    private ReasoningBuilder() {}

    /**
     * Generează explicația textuală a verdictului ML.
     *
     * @param flagged      true dacă tranzacția a fost marcată ca suspectă
     * @param anomalyScore scorul de anomalie brut din IsolationForest [0,1]
     * @param importances  importanțele per feature calculate de PerturbationAnalyzer
     * @return text descriptiv pentru audit trail / notificare utilizator
     */
    public static String build(boolean flagged, double anomalyScore, double[] importances) {
        double total = Arrays.stream(importances).sum();
        int topIdx    = MlUtils.argmax(importances);
        int secondIdx = MlUtils.argmax2(importances);
        int topPct    = total > 0 ? (int) (importances[topIdx]    / total * 100) : 0;
        int secondPct = total > 0 ? (int) (importances[secondIdx] / total * 100) : 0;

        if (flagged) {
            return String.format(
                "Transaction flagged as suspicious (anomaly score: %.2f). " +
                "Primary factor: %s (%d%%). Secondary factor: %s (%d%%).",
                anomalyScore, FEATURE_NAMES[topIdx], topPct,
                FEATURE_NAMES[secondIdx], secondPct);
        } else {
            return String.format(
                "Transaction considered normal (anomaly score: %.2f). " +
                "No factor exceeds the individual risk threshold.",
                anomalyScore);
        }
    }
}
