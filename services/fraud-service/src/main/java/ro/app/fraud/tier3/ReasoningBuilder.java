package ro.app.fraud.tier3;

import java.util.Arrays;

/**
 * Construiește textul de explicație al verdictului ML pentru utilizator/admin.
 *
 * Etichetele din FEATURE_NAMES sunt mapate 1:1 cu ordinea vectorului produs
 * de FraudFeatureEngine si asamblat de PaySimFeatureMapper / FeatureVectorBuilder:
 *
 *   [0] amountRatio          → "unusually large transaction amount"
 *   [1] typeRisk             → "high-risk transaction type (external / instant)"
 *   [2] hourSuspicion        → "transaction during suspicious night-time hours"
 *   [3] newAccountFlag       → "new or recently opened sender account"
 *   [4] senderDepletionRatio → "sender account heavily or fully drained"
 *   [5] isRoundAmount        → "suspiciously round transaction amount"
 *
 * IMPORTANT: Daca ordinea vectorului se schimba in FraudFeatureEngine,
 * actualizeaza si acest array in consecinta.
 */
public final class ReasoningBuilder {

    /**
     * Etichete human-readable aliniate 1:1 cu vectorul de 6 features PSD2.
     * Ordinea trebuie sa corespunda exact cu FraudFeatureEngine.
     */
    public static final String[] FEATURE_NAMES = {
        "unusually large transaction amount",                       // [0] amountRatio
        "high-risk transaction type (external / instant)",          // [1] typeRisk
        "transaction during suspicious night-time hours",           // [2] hourSuspicion
        "new or recently opened sender account",                    // [3] newAccountFlag
        "sender account heavily or fully drained",                  // [4] senderDepletionRatio
        "suspiciously round transaction amount"                     // [5] isRoundAmount
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
