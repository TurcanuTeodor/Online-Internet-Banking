package ro.app.fraud.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import ro.app.fraud.tier3.MlVerdict;
import ro.app.fraud.tier3.Tier3MlService;

@Component
public class Tier2AsyncRunner {

    private static final Logger log = LoggerFactory.getLogger(Tier2AsyncRunner.class);

    private final FraudDecisionRepository decisionRepo;
    private final BehavioralScoringService scoringService;
    private final BehaviorProfileService profileService;
    private final TransactionRestClient transactionClient;

    @Autowired(required = false)
    private Tier3MlService tier3;

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
                applyHighRiskVerdict(decisionId, decision, scoring, req.getClientId());
            } else if (scoring.totalScore() >= 30) {
                log.info("Tier2 ambiguous (score={}), escalating to Tier3 ML", scoring.totalScore());
                applyAmbiguousVerdict(decisionId, decision, req, scoring);
            } else {
                applyLowRiskVerdict(decisionId, decision, scoring);
            }

            decisionRepo.save(decision);
            log.info("Tier2 async complete: decision={} finalStatus={} score={}",
                    decisionId, decision.getStatus(), scoring.totalScore());

        } catch (Exception e) {
            log.error("Tier2 async failed for decision {}: {}", decisionId, e.getMessage(), e);
        }
    }

    // ── Verdict helpers ──────────────────────────────────────────────────────

    private void applyHighRiskVerdict(Long decisionId, FraudDecision decision,
                                      ScoringResult scoring, Long clientId) {
        decision.setStatus(FraudDecisionStatus.FLAG);
        decision.setExplanation("Tier2 HIGH RISK: " + scoring.summary());
        log.warn("FLAGGED: decision={} score={} client={}", decisionId, scoring.totalScore(), clientId);
    }

    private void applyAmbiguousVerdict(Long decisionId, FraudDecision decision,
                                       FraudEvaluationRequest req, ScoringResult scoring) {
        if (tier3 != null) {
            MlVerdict mlVerdict;
            try {
                mlVerdict = tier3.analyze(decisionId, req, scoring);
            } catch (Exception e) {
                log.error("Tier3 analysis failed for decision {}: {} — falling back to Tier2 decision", decisionId, e.getMessage());
                applyTier3KillSwitchFallback(decisionId, decision, scoring);
                return;
            }
            decision.setDecidedByTier(FraudTier.TIER3_ML);
            applyMlVerdict(decisionId, decision, mlVerdict);
        } else {
            applyTier3KillSwitchFallback(decisionId, decision, scoring);
        }
    }

    private void applyMlVerdict(Long decisionId, FraudDecision decision, MlVerdict mlVerdict) {
        if (mlVerdict.isFlagged()) {
            decision.setStatus(FraudDecisionStatus.FLAG);
            decision.setExplanation("Tier3-ML FLAG (confidence=" +
                    String.format("%.0f%%", mlVerdict.confidence() * 100) + "): " + mlVerdict.reasoning());
            log.warn("Tier3-ML FLAGGED: decision={} confidence={} reason={}",
                    decisionId, mlVerdict.confidence(), mlVerdict.reasoning());
        } else {
            decision.setStatus(FraudDecisionStatus.ALLOW);
            decision.setExplanation("Tier3-ML ALLOW: " + mlVerdict.reasoning());
            log.info("Tier3-ML ALLOW: decision={} confidence={}", decisionId, mlVerdict.confidence());
        }
    }

    /** Called when {@code fraud.tier3.ml.enabled=false} — Tier 2 makes the final call. */
    private void applyTier3KillSwitchFallback(Long decisionId, FraudDecision decision, ScoringResult scoring) {
        log.info("Tier3 disabled — Tier2 final decision: score={}", scoring.totalScore());
        decision.setStatus(scoring.totalScore() >= 50 ? FraudDecisionStatus.FLAG : FraudDecisionStatus.ALLOW);
        decision.setDecidedByTier(FraudTier.TIER2_BEHAVIORAL);
        decision.setExplanation("Tier2 final (Tier3 disabled): " + scoring.summary());
    }

    private void applyLowRiskVerdict(Long decisionId, FraudDecision decision, ScoringResult scoring) {
        decision.setStatus(FraudDecisionStatus.ALLOW);
        decision.setDecidedByTier(FraudTier.TIER2_BEHAVIORAL);
        decision.setExplanation("Tier2 low risk: " + scoring.summary());
        log.info("Tier2 low risk: decision={} score={}", decisionId, scoring.totalScore());
    }
}
