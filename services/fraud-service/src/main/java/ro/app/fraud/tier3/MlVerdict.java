package ro.app.fraud.tier3;

public record MlVerdict(
        String verdict,
        double confidence,
        String reasoning,
        double anomalyScore
) {
    public boolean isFlagged() {
        return "FLAG".equalsIgnoreCase(verdict) || "BLOCK".equalsIgnoreCase(verdict);
    }
}
