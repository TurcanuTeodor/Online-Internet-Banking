package ro.app.fraud.tier1;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.micrometer.observation.annotation.Observed;

import ro.app.fraud.dto.FraudEvaluationRequest;
import ro.app.fraud.repository.FraudDecisionRepository;

/**
 * Tier 1 — Deterministic rule engine. Runs synchronously on every transaction.
 * Rules are evaluated in order; first BLOCK wins. If no BLOCK, result is ALLOW or REVIEW.
 */
@Component
public class RuleEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleEngine.class);

    private static final double LARGE_AMOUNT_THRESHOLD = 10_000.0;
    private static final int BURST_LIMIT = 5;
    private static final int NEW_ACCOUNT_DAYS = 30;
    private static final double NEW_ACCOUNT_AMOUNT_THRESHOLD = 2_000.0;

    private final FraudDecisionRepository decisionRepo;

    public RuleEngine(FraudDecisionRepository decisionRepo) {
        this.decisionRepo = decisionRepo;
    }

    @Observed(name = "fraud.tier1.latency", contextualName = "tier1-evaluation")
    public RuleResult evaluate(FraudEvaluationRequest req) {
        log.info("Tier1 evaluating: client={} amount={} account={}", req.getClientId(), req.getAmount(), req.getAccountId());

        if (req.isSelfTransfer()) {
            log.debug("Self-transfer detected — auto-ALLOW");
            return RuleResult.allow();
        }

        List<String> triggers = new ArrayList<>();
        double maxRisk = 0.0;

        // Rule 1: Very large amount
        if (req.getAmount() > LARGE_AMOUNT_THRESHOLD) {
            double risk = Math.min(100.0, 70.0 + (req.getAmount() - LARGE_AMOUNT_THRESHOLD) / 1000.0);
            maxRisk = Math.max(maxRisk, risk);
            triggers.add("LARGE_AMOUNT(>" + LARGE_AMOUNT_THRESHOLD + ")");
            log.info("Rule hit: LARGE_AMOUNT — {} > {}", req.getAmount(), LARGE_AMOUNT_THRESHOLD);
        }

        // Rule 2: Burst — 5+ evaluations for same account in last 60 seconds
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        long recentCount = decisionRepo.countByAccountIdAndCreatedAtAfter(req.getAccountId(), oneMinuteAgo);
        if (recentCount >= BURST_LIMIT) {
            maxRisk = Math.max(maxRisk, 90.0);
            triggers.add("BURST(" + (recentCount + 1) + " tx/min)");
            log.info("Rule hit: BURST — {} recent transactions for account {}", recentCount, req.getAccountId());
        }

        // Rule 3: New account + large-ish amount
        if (req.getAccountAgeDays() < NEW_ACCOUNT_DAYS && req.getAmount() > NEW_ACCOUNT_AMOUNT_THRESHOLD) {
            maxRisk = Math.max(maxRisk, 75.0);
            triggers.add("NEW_ACCOUNT_HIGH_AMOUNT(age=" + req.getAccountAgeDays() + "d,amount=" + req.getAmount() + ")");
            log.info("Rule hit: NEW_ACCOUNT_HIGH_AMOUNT — age {}d, amount {}", req.getAccountAgeDays(), req.getAmount());
        }

        if (triggers.isEmpty()) {
            return RuleResult.allow(); //no suspicious rules hit in Tier 1
        }

        String ruleHits = String.join(", ", triggers);
        String explanation = "Flagged by Tier 1 rules: " + ruleHits + ". Step-up authentication required.";

        return RuleResult.stepUp(maxRisk, ruleHits, explanation);
    }
}
