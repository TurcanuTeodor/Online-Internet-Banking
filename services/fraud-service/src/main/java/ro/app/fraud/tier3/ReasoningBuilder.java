package ro.app.fraud.tier3;

import java.util.Arrays;

/**
 * Construiește textul de explicație al verdictului ML pentru utilizator/admin.
 */
public final class ReasoningBuilder {

    
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
