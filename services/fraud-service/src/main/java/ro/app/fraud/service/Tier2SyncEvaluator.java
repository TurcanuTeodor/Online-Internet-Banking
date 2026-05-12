package ro.app.fraud.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ro.app.fraud.client.ExternalTransactionDto;
import ro.app.fraud.client.TransactionRestClient;
import ro.app.fraud.config.FraudProperties;
import ro.app.fraud.dto.FraudEvaluationRequest;
import ro.app.fraud.model.entity.UserBehaviorProfile;
import ro.app.fraud.model.enums.FraudDecisionStatus;
import ro.app.fraud.model.enums.FraudTier;
import ro.app.fraud.repository.FraudDecisionRepository;
import ro.app.fraud.tier2.BehavioralScoringService;
import ro.app.fraud.tier2.ScoringResult;

/**
 * Synchronous Tier 2 evaluation for transfer decisions.
 * Called before returning response to account-service,
 * so STEP_UP_REQUIRED can be enforced on the caller side.
 *
 * Tier 3 ML remains async (see Tier2AsyncRunner) — it updates
 * the decision record after the fact for learning and alerting,
 * but does NOT block the synchronous flow.
 */

@Service
public class Tier2SyncEvaluator {
    
    private static final Logger log = LoggerFactory.getLogger(Tier2SyncEvaluator.class);

    private final TransactionRestClient transactionClient;
    private final BehaviorProfileService profileService;
    private final BehavioralScoringService scoringService;
    private final FraudDecisionRepository decisionRepo;
    private final double lowerThreshold; // 30.0
    private final double upperThreshold; // 70.0
    private final double stepUpThreshold; // 50.0 - middle ground for STEP_UP_REQUIRED

    public Tier2SyncEvaluator(TransactionRestClient transactionClient,
                              BehaviorProfileService profileService,
                              BehavioralScoringService scoringService,
                              FraudDecisionRepository decisionRepo,
                              FraudProperties fraudProperties) {
        this.transactionClient = transactionClient;
        this.profileService = profileService;
        this.scoringService = scoringService;
        this.decisionRepo = decisionRepo;
        FraudProperties.Tier2 tier2= fraudProperties.getTier2();
        this.lowerThreshold = tier2.getLowerThreshold();
        this.upperThreshold = tier2.getUpperThreshold();
        this.stepUpThreshold = (lowerThreshold + upperThreshold) / 2.0; // 50.0
    }

    /**
     * Runs Tier 2 synchronously and returns the decision status.
     * Updates the FraudDecision record in DB with Tier 2 results.
     *
     * @return FraudDecisionStatus to return to account-service:
     *         ALLOW, STEP_UP_REQUIRED, or FLAG
     */

    public FraudDecisionStatus evaluate(Long decisionId, FraudEvaluationRequest req) {
        
        try{
            log.info("Tier2Sync start: decisionId={} client={} account={}",
                    decisionId, req.getClientId(), req.getAccountId());
            

        // Fetch recent transactions for context 
        List<ExternalTransactionDto> history = transactionClient.getTransactionsByAccount(req.getAccountId());

        // Recompute user behavior profile based on latest transactions
        UserBehaviorProfile profile = profileService.recompute(req.getClientId(), history);

        // Compute behavioral risk score
        ScoringResult scoring = scoringService.score(req, history, profile);

        // Map score to decision status using thresholds
        FraudDecisionStatus status = mapScoreToStatus(scoring.totalScore());

        // Persist Tier 2 results
        decisionRepo.findById(decisionId).ifPresent(decision -> {
            decision.setDecidedByTier(FraudTier.TIER2_BEHAVIORAL);
            decision.setRiskScore(scoring.totalScore());
            decision.setRuleHits(scoring.summary());
            decision.setStatus(status);
            decision.setExplanation(buildExplanation(status, scoring));
            decisionRepo.save(decision);
        });

        log.info("Tier2Sync complete: decision={} score={} status={}",
                    decisionId, String.format("%.1f", scoring.totalScore()), status);

            return status;

        }catch(Exception e){
            // Fail-open: if Tier 2 sync fails, don't block the transfer
            log.error("Tier2Sync failed for decision {}: {} — failing open (ALLOW)",
                    decisionId, e.getMessage());
            return FraudDecisionStatus.ALLOW;
        }
    }

    private FraudDecisionStatus mapScoreToStatus(double score){
        if(score >= upperThreshold){
            // High risk — flag but don't block (account-service decides)
            // FLAG means: transfer proceeds but decision is recorded for review
            return FraudDecisionStatus.FLAG;
        }
        else if(score >= stepUpThreshold){
            // Medium-high risk — require step-up authentication before proceeding
            return FraudDecisionStatus.STEP_UP_REQUIRED;
        }
        else{
            // Low risk — allow transfer to proceed
            return FraudDecisionStatus.ALLOW;
        }
    }
    
    private String buildExplanation(FraudDecisionStatus status, ScoringResult scoring){
        return String.format("Tier2Sync %s: score=%.1f. %s",
            status.name(), scoring.totalScore(), scoring.summary());
    }
}
