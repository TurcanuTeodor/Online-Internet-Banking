package ro.app.fraud.tier2;

import java.util.Map;

public record ScoringResult(
        double totalScore,
        Map<String, Double> componentScores,
        String summary
) {
}
