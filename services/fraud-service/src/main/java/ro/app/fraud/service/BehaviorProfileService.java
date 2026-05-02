package ro.app.fraud.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ro.app.fraud.client.ExternalTransactionDto;
import ro.app.fraud.model.entity.UserBehaviorProfile;
import ro.app.fraud.repository.UserBehaviorProfileRepository;

@Service
public class BehaviorProfileService {

    private static final Logger log = LoggerFactory.getLogger(BehaviorProfileService.class);

    private final UserBehaviorProfileRepository profileRepo;

    public BehaviorProfileService(UserBehaviorProfileRepository profileRepo) {
        this.profileRepo = profileRepo;
    }

    public UserBehaviorProfile getOrCreate(Long clientId) {
        return profileRepo.findByClientId(clientId).orElseGet(() -> {
            UserBehaviorProfile p = new UserBehaviorProfile();
            p.setClientId(clientId);
            return profileRepo.save(p);
        });
    }

    /**
     * Recompute profile from full transaction history.
     */
    public UserBehaviorProfile recompute(Long clientId, List<ExternalTransactionDto> history) {
        UserBehaviorProfile profile = getOrCreate(clientId);

        List<ExternalTransactionDto> outgoing = history.stream()
                .filter(tx -> "-".equals(tx.getSign()))
                .toList();

        if (outgoing.isEmpty()) {
            profile.setLastUpdated(LocalDateTime.now());
            return profileRepo.save(profile);
        }

        DoubleSummaryStatistics stats = outgoing.stream()
                .mapToDouble(tx -> tx.getAmount().doubleValue())
                .summaryStatistics();
        double avg = stats.getAverage();
        double max = stats.getMax();

        profile.setAvgTransactionAmount(avg);
        profile.setMaxTransactionAmount(max);
        profile.setTransactionCount(outgoing.size());
        profile.setAvgDailyTransactions(computeAvgDaily(outgoing));

        int peakStart = computePeakHourStart(outgoing);
        profile.setTypicalHourStart(peakStart);
        profile.setTypicalHourEnd(Math.min(23, peakStart + 6));

        Set<String> frequentIbans = extractFrequentIbans(outgoing);
        if (!frequentIbans.isEmpty()) {
            profile.setCommonIbans(String.join(",", frequentIbans));
        }

        profile.setLastUpdated(LocalDateTime.now());
        profile = profileRepo.save(profile);

        log.info("Profile updated: client={} avg={} max={} txCount={} avgDaily={}",
                clientId, String.format("%.2f", avg), String.format("%.2f", max),
                outgoing.size(), String.format("%.1f", profile.getAvgDailyTransactions()));

        return profile;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private double computeAvgDaily(List<ExternalTransactionDto> outgoing) {
        Set<java.time.LocalDate> uniqueDays = outgoing.stream()
                .filter(tx -> tx.getTransactionDate() != null)
                .map(tx -> tx.getTransactionDate().toLocalDate())
                .collect(Collectors.toSet());
        return uniqueDays.isEmpty() ? outgoing.size() : (double) outgoing.size() / uniqueDays.size();
    }

    /** Finds the hour of day with the most transactions, then widens it into a ±2h window. */
    private int computePeakHourStart(List<ExternalTransactionDto> outgoing) {
        int[] hourBuckets = new int[24];
        outgoing.stream()
                .filter(tx -> tx.getTransactionDate() != null)
                .forEach(tx -> hourBuckets[tx.getTransactionDate().getHour()]++);

        int peakHour = 8;
        int maxCount = 0;
        for (int h = 0; h < 24; h++) {
            if (hourBuckets[h] > maxCount) {
                maxCount = hourBuckets[h];
                peakHour = h;
            }
        }
        return peakHour > 3 ? Math.max(0, peakHour - 2) : peakHour;
    }

    private Set<String> extractFrequentIbans(List<ExternalTransactionDto> outgoing) {
        return outgoing.stream()
                .filter(tx -> tx.getDetails() != null)
                .map(this::parseIbanFromDetails)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /** Extracts a Romanian IBAN (24 chars starting with "RO") from a free-text details field. */
    private String parseIbanFromDetails(ExternalTransactionDto tx) {
        String details = tx.getDetails();
        int idx = details.indexOf("RO");
        if (idx >= 0 && details.length() >= idx + 24) {
            return details.substring(idx, Math.min(details.length(), idx + 24));
        }
        return null;
    }
}
