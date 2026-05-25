package ro.app.fraud.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
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
@Profile("!trainer") // nu se instantiaza in modul trainer (nu exista JPA/repository)
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

    /**
     * Runs only Tier 3 ML async — Tier 2 already ran synchronously.
     * Used when Tier 2 returned ALLOW and we want ML validation
     * for learning/alerting purposes without blocking the transfer.
     */
    @Async("fraudAsyncExecutor")
    public void runTier3Only(Long decisionId, FraudEvaluationRequest req) {
        try {
            log.info("Tier3 async start: decision={}", decisionId);

            FraudDecision decision = decisionRepo.findById(decisionId).orElse(null);
            if (decision == null) {
                log.warn("Decision {} not found for Tier3 update", decisionId);
                return;
            }

            // Need scoring result for Tier 3 feature vector
            // Re-fetch history (already fetched in Tier2Sync, but async so we fetch again)
            List<ExternalTransactionDto> history =
                    transactionClient.getTransactionsByAccount(req.getAccountId());
            UserBehaviorProfile profile =
                    profileService.getOrCreate(req.getClientId());
            ScoringResult scoring =
                    scoringService.score(req, history, profile);

            if (tier3 == null) {
                log.debug("Tier3 disabled — skipping async ML analysis");
                return;
            }

            MlVerdict mlVerdict;
            try {
                mlVerdict = tier3.analyze(decisionId, req, scoring);
            } catch (Exception e) {
                log.error("Tier3 async failed for decision {}: {}", decisionId, e.getMessage());
                return;
            }

            // If Tier 3 flags something Tier 2 missed, update record for review
            // NOTE: this does NOT affect the transfer — it's already processed
            // This creates an alert for admin review
            if (mlVerdict.isFlagged()) {
                decision.setDecidedByTier(FraudTier.TIER3_ML);
                decision.setStatus(FraudDecisionStatus.FLAG);
                decision.setExplanation("Tier3-ML post-hoc FLAG: " + mlVerdict.reasoning());
                decision.setRiskScore(mlVerdict.anomalyScore() * 100.0);
                decision.setRuleHits(String.format("Isolation Forest Anomaly Score: %.2f (Confidence: %.2f)", mlVerdict.anomalyScore() * 100.0, mlVerdict.confidence() * 100.0));
                decisionRepo.save(decision);
                log.warn("Tier3 async POST-HOC FLAG: decision={} confidence={}",
                        decisionId, mlVerdict.confidence());
            } else {
                log.info("Tier3 async ALLOW: decision={}", decisionId);
            }

        } catch (Exception e) {
            log.error("Tier3 async runner failed for decision {}: {}", decisionId, e.getMessage());
        }
    }
}
