package ro.app.fraud.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
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

        double sum = outgoing.stream().mapToDouble(tx -> tx.getAmount().doubleValue()).sum();
        double max = outgoing.stream().mapToDouble(tx -> tx.getAmount().doubleValue()).max().orElse(0.0);
        double avg = sum / outgoing.size();

        profile.setAvgTransactionAmount(avg);
        profile.setMaxTransactionAmount(max);
        profile.setTransactionCount(outgoing.size());

        Set<java.time.LocalDate> uniqueDays = outgoing.stream()
                .filter(tx -> tx.getTransactionDate() != null)
                .map(tx -> tx.getTransactionDate().toLocalDate())
                .collect(Collectors.toSet());
        double avgDaily = uniqueDays.isEmpty() ? outgoing.size() : (double) outgoing.size() / uniqueDays.size();
        profile.setAvgDailyTransactions(avgDaily);

        int[] hourBuckets = new int[24];
        outgoing.stream()
                .filter(tx -> tx.getTransactionDate() != null)
                .forEach(tx -> hourBuckets[tx.getTransactionDate().getHour()]++);

        int peakStart = 8, peakEnd = 22;
        int maxCount = 0;
        for (int h = 0; h < 24; h++) {
            if (hourBuckets[h] > maxCount) {
                maxCount = hourBuckets[h];
                peakStart = h;
            }
        }
        peakEnd = Math.min(23, peakStart + 6);
        if (peakStart > 3) peakStart = Math.max(0, peakStart - 2);
        profile.setTypicalHourStart(peakStart);
        profile.setTypicalHourEnd(peakEnd);

        Set<String> frequentIbans = outgoing.stream()
                .filter(tx -> tx.getDetails() != null)
                .map(tx -> {
                    String d = tx.getDetails();
                    int idx = d.indexOf("RO");
                    if (idx >= 0 && d.length() >= idx + 24) {
                        return d.substring(idx, Math.min(d.length(), idx + 24));
                    }
                    return null;
                })
                .filter(iban -> iban != null)
                .collect(Collectors.toSet());

        if (!frequentIbans.isEmpty()) {
            profile.setCommonIbans(String.join(",", frequentIbans));
        }

        profile.setLastUpdated(LocalDateTime.now());
        profile = profileRepo.save(profile);

        log.info("Profile updated: client={} avg={} max={} txCount={} avgDaily={}",
                clientId, String.format("%.2f", avg), String.format("%.2f", max),
                outgoing.size(), String.format("%.1f", avgDaily));

        return profile;
    }
}
