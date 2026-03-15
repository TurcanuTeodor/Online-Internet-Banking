package ro.app.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limit service for login attempts per IP address.
 * Max 5 failed attempts per minute per IP.
 */
@Service
public class RateLimitService {
    
    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration RESET_DURATION = Duration.ofMinutes(1);
    
    // Bucket4j token buckets per IP
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    // Track failed attempts per IP
    private final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    
    public void validateLoginAttempt(String clientIp) {
        if (failedAttempts.getOrDefault(clientIp, 0) >= MAX_FAILED_ATTEMPTS) {
            log.warn("Login attempt blocked for IP {} due to rate limiting", clientIp);
            throw new IllegalArgumentException("Too many failed login attempts. Please try again later.");
        }
    }
    
    public void recordFailedAttempt(String clientIp) {
        int attempts = failedAttempts.getOrDefault(clientIp, 0) + 1;
        failedAttempts.put(clientIp, attempts);
        log.warn("Failed login attempt for IP {}: {} attempts", clientIp, attempts);
    }
    
    public void recordSuccessfulAttempt(String clientIp) {
        failedAttempts.remove(clientIp);
        log.info("Successful login for IP {}", clientIp);
    }
    
    /**
     * Reset failed attempts counter every minute (called automatically by scheduler)
     */
    @Scheduled(fixedDelay = 60000) // Run every 1 minute
    public void resetFailedAttemptsCounter() {
        if (!failedAttempts.isEmpty()) {
            int size = failedAttempts.size();
            failedAttempts.clear();
            log.debug("Reset failed login attempts for {} IPs", size);
        }
    }
}
