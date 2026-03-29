package ro.app.fraud.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ro.app.fraud.client.ExternalTransactionDto;
import ro.app.fraud.client.TransactionRestClient;
import ro.app.fraud.dto.FraudEvaluationRequest;
import ro.app.fraud.model.entity.FraudDecision;
import ro.app.fraud.model.entity.UserBehaviorProfile;
import ro.app.fraud.model.enums.FraudDecisionStatus;
import ro.app.fraud.model.enums.FraudTier;
import ro.app.fraud.repository.FraudDecisionRepository;
import ro.app.fraud.tier2.BehavioralScoringService;
import ro.app.fraud.tier2.ScoringResult;

/**
 * Separate bean so Spring's @Async proxy works (no self-invocation).
 */
@Component
public class Tier2AsyncRunner {

    private static final Logger log = LoggerFactory.getLogger(Tier2AsyncRunner.class);

    private final FraudDecisionRepository decisionRepo;
    private final BehavioralScoringService scoringService;
    private final BehaviorProfileService profileService;
    private final TransactionRestClient transactionClient;

    public Tier2AsyncRunner(FraudDecisionRepository decisionRepo,
                            BehavioralScoringService scoringService,
                            BehaviorProfileService profileService,
                            TransactionRestClient transactionClient) {
        this.decisionRepo = decisionRepo;
        this.scoringService = scoringService;
        this.profileService = profileService;
        this.transactionClient = transactionClient;
    }

    @Async("fraudAsyncExecutor")
    public void run(Long decisionId, FraudEvaluationRequest req) {
        try {
            log.info("Tier2 async start: decision={} client={} account={}",
                    decisionId, req.getClientId(), req.getAccountId());

            List<ExternalTransactionDto> history = transactionClient.getTransactionsByAccount(req.getAccountId());
            log.info("Fetched {} historical transactions for account {}", history.size(), req.getAccountId());

            UserBehaviorProfile profile = profileService.recompute(req.getClientId(), history);

            ScoringResult scoring = scoringService.score(req, history, profile);

            FraudDecision decision = decisionRepo.findById(decisionId).orElse(null);
            if (decision == null) {
                log.warn("Decision {} not found for Tier2 update", decisionId);
                return;
            }

            decision.setDecidedByTier(FraudTier.TIER2_BEHAVIORAL);
            decision.setRiskScore(scoring.totalScore());
            decision.setRuleHits(scoring.summary());

            if (scoring.totalScore() >= 70) {
                decision.setStatus(FraudDecisionStatus.FLAG);
                decision.setExplanation("Tier2 HIGH RISK: " + scoring.summary());
                log.warn("FLAGGED: decision={} score={} client={}", decisionId, scoring.totalScore(), req.getClientId());
            } else if (scoring.totalScore() >= 30) {
                decision.setStatus(FraudDecisionStatus.ALLOW);
                decision.setExplanation("Tier2 medium risk (logged): " + scoring.summary());
                log.info("Tier2 medium risk: decision={} score={}", decisionId, scoring.totalScore());
            } else {
                decision.setStatus(FraudDecisionStatus.ALLOW);
                decision.setExplanation("Tier2 low risk: " + scoring.summary());
                log.info("Tier2 low risk: decision={} score={}", decisionId, scoring.totalScore());
            }

            decisionRepo.save(decision);
            log.info("Tier2 async complete: decision={} finalStatus={} score={}",
                    decisionId, decision.getStatus(), scoring.totalScore());

        } catch (Exception e) {
            log.error("Tier2 async failed for decision {}: {}", decisionId, e.getMessage(), e);
        }
    }
}
