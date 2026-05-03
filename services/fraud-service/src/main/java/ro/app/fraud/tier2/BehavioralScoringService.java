package ro.app.fraud.tier2;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.micrometer.observation.annotation.Observed;

import ro.app.fraud.client.ExternalTransactionDto;
import ro.app.fraud.dto.FraudEvaluationRequest;
import ro.app.fraud.model.entity.UserBehaviorProfile;

/**
 * Tier 2 — Behavioral risk scoring. Computes a 0–100 risk score by comparing the
 * current transaction against the user's historical behavioral profile.
 */
@Service
public class BehavioralScoringService {

    private static final Logger log = LoggerFactory.getLogger(BehavioralScoringService.class);

    private static final double W_AMOUNT    = 0.30;
    private static final double W_FREQUENCY = 0.20;
    private static final double W_TIME      = 0.15;
    private static final double W_RECIPIENT = 0.15;
    private static final double W_CATEGORY  = 0.10;
    private static final double W_VELOCITY  = 0.10;

    @Observed(name = "fraud.tier2.latency", contextualName = "tier2-scoring")
    public ScoringResult score(FraudEvaluationRequest req,
                               List<ExternalTransactionDto> history,
                               UserBehaviorProfile profile) {

        Map<String, Double> components = new LinkedHashMap<>();

        double amountScore    = scoreAmount(req.getAmount(), profile, history);
        double frequencyScore = scoreFrequency(history);
        double timeScore      = scoreTime(profile);
        double recipientScore = scoreRecipient(req, history);
        double categoryScore  = scoreCategoryRisk(req.getTransactionType());
        double velocityScore  = scoreVelocity(req.getAmount(), history);

        // NOTE: these key names are coupled to FeatureVectorBuilder in tier3 —
        // if you rename a key here, update FeatureVectorBuilder.build() accordingly.
        components.put("amount_anomaly",    amountScore);
        components.put("frequency_anomaly", frequencyScore);
        components.put("time_anomaly",      timeScore);
        components.put("recipient_anomaly", recipientScore);
        components.put("category_risk",     categoryScore);
        components.put("velocity_24h",      velocityScore);

        double total = amountScore    * W_AMOUNT
                     + frequencyScore * W_FREQUENCY
                     + timeScore      * W_TIME
                     + recipientScore * W_RECIPIENT
                     + categoryScore  * W_CATEGORY
                     + velocityScore  * W_VELOCITY;

        total = Math.min(100.0, Math.max(0.0, total));

        String topFactors = components.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .map(e -> e.getKey() + "=" + String.format("%.0f", e.getValue()))
                .collect(Collectors.joining(", "));

        String summary = String.format("Tier2 score=%.1f top_factors=[%s]", total, topFactors);
        log.info(summary);

        return new ScoringResult(total, components, summary);
    }

    /**
     * Amount Anomaly (0–100): z-score of current amount vs user's history.
     */
    private double scoreAmount(double currentAmount, UserBehaviorProfile profile, List<ExternalTransactionDto> history) {
        if (profile.getTransactionCount() < 3) {
            return currentAmount > 1000 ? 50.0 : 10.0;
        }

        double mean = profile.getAvgTransactionAmount();
        double max = profile.getMaxTransactionAmount();
        if (mean <= 0) return 10.0;

        double stdDev = computeStdDev(history, mean);
        if (stdDev <= 0) stdDev = mean * 0.3;

        double zScore = (currentAmount - mean) / stdDev;

        if (zScore <= 1.0) return 5.0;
        if (zScore <= 2.0) return 30.0;
        if (zScore <= 3.0) return 60.0;
        return Math.min(100.0, 70.0 + (zScore - 3.0) * 10.0);
    }

    /**
     * Frequency Anomaly (0–100): tx count today vs. average daily.
     */
    private double scoreFrequency(List<ExternalTransactionDto> history) {
        if (history.isEmpty()) return 10.0;

        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        long todayCount = history.stream()
                .filter(tx -> tx.getTransactionDate() != null && tx.getTransactionDate().isAfter(startOfDay))
                .count();

        double avgDaily = computeAvgDailyCount(history);
        double ratio = todayCount / avgDaily;

        if (ratio <= 1.5) return 5.0;
        if (ratio <= 2.5) return 35.0;
        if (ratio <= 4.0) return 65.0;
        return Math.min(100.0, 75.0 + (ratio - 4.0) * 5.0);
    }

    /**
     * Time Anomaly (0–100): is the current hour unusual for this user?
     */
    private double scoreTime(UserBehaviorProfile profile) {
        int currentHour = LocalDateTime.now().getHour();
        int start = profile.getTypicalHourStart();
        int end = profile.getTypicalHourEnd();

        if (start <= end) {
            if (currentHour >= start && currentHour <= end) return 5.0;
        } else {
            if (currentHour >= start || currentHour <= end) return 5.0;
        }

        int distance;
        if (start <= end) {
            distance = Math.min(Math.abs(currentHour - start), Math.abs(currentHour - end));
        } else {
            int dStart = Math.min(Math.abs(currentHour - start), 24 - Math.abs(currentHour - start));
            int dEnd = Math.min(Math.abs(currentHour - end), 24 - Math.abs(currentHour - end));
            distance = Math.min(dStart, dEnd);
        }

        return Math.min(100.0, 30.0 + distance * 15.0);
    }

    /**
     * Recipient Anomaly (0–100): is this a new/unknown recipient?
     */
    private double scoreRecipient(FraudEvaluationRequest req, List<ExternalTransactionDto> history) {
        if (req.isSelfTransfer()) return 0.0;

        String receiverIban = req.getReceiverIban();
        if (receiverIban == null || receiverIban.isBlank()) return 30.0;

        boolean knownRecipient = history.stream()
                .filter(tx -> tx.getDetails() != null)
                .anyMatch(tx -> tx.getDetails().contains(receiverIban));

        if (knownRecipient) return 5.0;

        double amount = req.getAmount();
        if (amount > 2000) return 80.0;
        if (amount > 500) return 50.0;
        return 30.0;
    }

    /**
     * Category Risk (0–100): base risk per transaction category.
     */
    private double scoreCategoryRisk(String transactionType) {
        if (transactionType == null) return 20.0;
        return switch (transactionType.toUpperCase()) {
            case "TRANSFER_EXTERNAL" -> 40.0;
            case "TRANSFER_INTERNAL" -> 15.0;
            case "WITHDRAWAL" -> 30.0;
            case "DEPOSIT" -> 5.0;
            default -> 20.0;
        };
    }

    /**
     * Velocity (0–100): total amount in last 24h compared to 30-day average.
     */
    private double scoreVelocity(double currentAmount, List<ExternalTransactionDto> history) {
        LocalDateTime cutoff24h = LocalDateTime.now().minusHours(24);
        double sum24h = history.stream()
                .filter(tx -> tx.getTransactionDate() != null && tx.getTransactionDate().isAfter(cutoff24h))
                .filter(tx -> "-".equals(tx.getSign()))
                .mapToDouble(tx -> tx.getAmount().doubleValue())
                .sum() + currentAmount;

        LocalDateTime cutoff30d = LocalDateTime.now().minusDays(30);
        List<ExternalTransactionDto> last30d = history.stream()
                .filter(tx -> tx.getTransactionDate() != null && tx.getTransactionDate().isAfter(cutoff30d))
                .filter(tx -> "-".equals(tx.getSign()))
                .toList();

        if (last30d.isEmpty()) {
            return currentAmount > 1000 ? 50.0 : 15.0;
        }

        double avgDailySpend = computeAvgDailySpend(last30d);
        double ratio = sum24h / avgDailySpend;

        if (ratio <= 1.5) return 5.0;
        if (ratio <= 3.0) return 35.0;
        if (ratio <= 5.0) return 65.0;
        return Math.min(100.0, 75.0 + (ratio - 5.0) * 5.0);
    }

    // ── Shared computation helpers ────────────────────────────────────────────

    /** Average number of transactions per active day in the given history. */
    private double computeAvgDailyCount(List<ExternalTransactionDto> history) {
        Set<java.time.LocalDate> uniqueDays = history.stream()
                .filter(tx -> tx.getTransactionDate() != null)
                .map(tx -> tx.getTransactionDate().toLocalDate())
                .collect(Collectors.toSet());
        return uniqueDays.isEmpty() ? 1.0 : Math.max(1.0, (double) history.size() / uniqueDays.size());
    }

    /** Average daily outgoing spend across active days in the given list. */
    private double computeAvgDailySpend(List<ExternalTransactionDto> outgoing) {
        Set<java.time.LocalDate> days = outgoing.stream()
                .map(tx -> tx.getTransactionDate().toLocalDate())
                .collect(Collectors.toSet());
        double total = outgoing.stream().mapToDouble(tx -> tx.getAmount().doubleValue()).sum();
        double avg = days.isEmpty() ? total : total / days.size();
        return Math.max(1.0, avg);
    }

    private double computeStdDev(List<ExternalTransactionDto> history, double mean) {
        List<Double> amounts = history.stream()
                .filter(tx -> "-".equals(tx.getSign()))
                .map(tx -> tx.getAmount().doubleValue())
                .toList();
        if (amounts.size() < 2) return mean * 0.3;

        double sumSqDiff = amounts.stream().mapToDouble(a -> Math.pow(a - mean, 2)).sum();
        return Math.sqrt(sumSqDiff / (amounts.size() - 1));
    }
}
