package ro.app.fraud.tier3;

import java.util.Arrays;

public final class ReasoningBuilder {

    public static final String[] FEATURE_NAMES = {
        "unusually high amount compared to your history",
        "high behavioral risk score",
        "unusual transaction frequency in the last 24h",
        "unknown recipient",
        "unusual transaction hour for your patterns",
        "new account with suspicious activity"
    };

    private ReasoningBuilder() {}

    public static String build(boolean flagged, double anomalyScore, double[] importances) {
        double total  = Arrays.stream(importances).sum();
        int topIdx = MlUtils.argmax(importances);
        int secondIdx = MlUtils.argmax2(importances);
        int topPct = total > 0 ? (int)(importances[topIdx]    / total * 100) : 0;
        int secondPct = total > 0 ? (int)(importances[secondIdx] / total * 100) : 0;

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
