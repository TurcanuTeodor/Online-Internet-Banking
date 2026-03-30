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
import ro.app.fraud.tier3.LlmVerdict;
import ro.app.fraud.tier3.Tier3LlmService;

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
    private final Tier3LlmService tier3LlmService;

    public Tier2AsyncRunner(FraudDecisionRepository decisionRepo,
                            BehavioralScoringService scoringService,
                            BehaviorProfileService profileService,
                            TransactionRestClient transactionClient,
                            Tier3LlmService tier3LlmService) {
        this.decisionRepo = decisionRepo;
        this.scoringService = scoringService;
        this.profileService = profileService;
        this.transactionClient = transactionClient;
        this.tier3LlmService = tier3LlmService;
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
                decision.setDecidedByTier(FraudTier.TIER2_BEHAVIORAL);
                decision.setExplanation("Tier2 HIGH RISK: " + scoring.summary());
                log.warn("FLAGGED: decision={} score={} client={}", decisionId, scoring.totalScore(), req.getClientId());
            } else if (scoring.totalScore() >= 30) {
                log.info("Tier2 ambiguous (score={}), escalating to Tier3 LLM", scoring.totalScore());
                LlmVerdict llmVerdict = tier3LlmService.analyze(req, scoring);
                decision.setDecidedByTier(FraudTier.TIER3_LLM);
                decision.setRiskScore(scoring.totalScore());
                if (llmVerdict.isFlagged()) {
                    decision.setStatus(FraudDecisionStatus.FLAG);
                    decision.setExplanation("Tier3 LLM FLAG (confidence=" +
                            String.format("%.0f%%", llmVerdict.confidence() * 100) + "): " + llmVerdict.reasoning());
                    log.warn("Tier3 FLAGGED: decision={} confidence={} reason={}",
                            decisionId, llmVerdict.confidence(), llmVerdict.reasoning());
                } else {
                    decision.setStatus(FraudDecisionStatus.ALLOW);
                    decision.setExplanation("Tier3 LLM ALLOW (confidence=" +
                            String.format("%.0f%%", llmVerdict.confidence() * 100) + "): " + llmVerdict.reasoning());
                    log.info("Tier3 ALLOW: decision={} confidence={}", decisionId, llmVerdict.confidence());
                }
            } else {
                decision.setStatus(FraudDecisionStatus.ALLOW);
                decision.setDecidedByTier(FraudTier.TIER2_BEHAVIORAL);
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
