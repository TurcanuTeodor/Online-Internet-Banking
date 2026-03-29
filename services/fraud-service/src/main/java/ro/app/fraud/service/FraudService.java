package ro.app.fraud.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import ro.app.fraud.dto.FraudDecisionDTO;
import ro.app.fraud.dto.FraudEvaluationRequest;
import ro.app.fraud.dto.FraudEvaluationResponse;
import ro.app.fraud.model.entity.FraudDecision;
import ro.app.fraud.model.enums.FraudDecisionStatus;
import ro.app.fraud.model.enums.FraudTier;
import ro.app.fraud.repository.FraudDecisionRepository;
import ro.app.fraud.repository.UserBehaviorProfileRepository;
import ro.app.fraud.tier1.RuleEngine;
import ro.app.fraud.tier1.RuleResult;

@Service
public class FraudService {

    private static final Logger log = LoggerFactory.getLogger(FraudService.class);

    private final FraudDecisionRepository decisionRepo;
    private final UserBehaviorProfileRepository profileRepo;
    private final RuleEngine ruleEngine;
    private final Tier2AsyncRunner tier2Runner;

    public FraudService(FraudDecisionRepository decisionRepo,
                        UserBehaviorProfileRepository profileRepo,
                        RuleEngine ruleEngine,
                        Tier2AsyncRunner tier2Runner) {
        this.decisionRepo = decisionRepo;
        this.profileRepo = profileRepo;
        this.ruleEngine = ruleEngine;
        this.tier2Runner = tier2Runner;
    }

    /**
     * Main entry point — called synchronously by account-service before committing a transfer.
     * Tier 1 runs synchronously. If MANUAL_REVIEW → async Tier 2 kicks off in background.
     */
    public FraudEvaluationResponse evaluate(FraudEvaluationRequest req) {
        log.info("Evaluating transfer: client={} amount={} {} -> {}",
                req.getClientId(), req.getAmount(), req.getSenderIban(), req.getReceiverIban());

        RuleResult tier1 = ruleEngine.evaluate(req);
        log.info("Tier1 result: status={} riskScore={} ruleHits={}",
                tier1.status(), tier1.riskScore(), tier1.ruleHits());

        FraudDecision decision = new FraudDecision();
        decision.setAccountId(req.getAccountId());
        decision.setClientId(req.getClientId());
        decision.setStatus(tier1.status());
        decision.setDecidedByTier(FraudTier.TIER1_RULES);
        decision.setRiskScore(tier1.riskScore());
        decision.setRuleHits(tier1.ruleHits());
        decision.setExplanation(tier1.explanation());

        decision = decisionRepo.save(decision);

        if (tier1.status() == FraudDecisionStatus.MANUAL_REVIEW) {
            tier2Runner.run(decision.getId(), req);
        }

        FraudEvaluationResponse resp = new FraudEvaluationResponse();
        resp.setDecisionId(decision.getId());
        resp.setStatus(decision.getStatus());
        resp.setDecidedByTier(decision.getDecidedByTier());
        resp.setRiskScore(decision.getRiskScore());
        resp.setRuleHits(decision.getRuleHits());
        resp.setExplanation(decision.getExplanation());

        return resp;
    }

    public FraudDecisionDTO getDecision(Long id) {
        FraudDecision d = decisionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Decision not found: " + id));
        return toDto(d);
    }

    public FraudDecisionDTO getByTransactionId(Long transactionId) {
        FraudDecision d = decisionRepo.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("No decision for txn: " + transactionId));
        return toDto(d);
    }

    public Page<FraudDecisionDTO> getAlerts(Pageable pageable) {
        List<FraudDecisionStatus> alertStatuses = List.of(
                FraudDecisionStatus.FLAG,
                FraudDecisionStatus.BLOCK,
                FraudDecisionStatus.MANUAL_REVIEW
        );
        return decisionRepo.findByStatusIn(alertStatuses, pageable).map(this::toDto);
    }

    public FraudDecisionDTO adminReview(Long decisionId, String adminUsername, String notes, FraudDecisionStatus newStatus) {
        FraudDecision d = decisionRepo.findById(decisionId)
                .orElseThrow(() -> new RuntimeException("Decision not found: " + decisionId));
        d.setReviewedByAdmin(adminUsername);
        d.setAdminNotes(notes);
        d.setStatus(newStatus);
        d = decisionRepo.save(d);
        return toDto(d);
    }

    private FraudDecisionDTO toDto(FraudDecision d) {
        FraudDecisionDTO dto = new FraudDecisionDTO();
        dto.setId(d.getId());
        dto.setTransactionId(d.getTransactionId());
        dto.setAccountId(d.getAccountId());
        dto.setClientId(d.getClientId());
        dto.setStatus(d.getStatus());
        dto.setDecidedByTier(d.getDecidedByTier());
        dto.setRiskScore(d.getRiskScore());
        dto.setRuleHits(d.getRuleHits());
        dto.setExplanation(d.getExplanation());
        dto.setReviewedByAdmin(d.getReviewedByAdmin());
        dto.setAdminNotes(d.getAdminNotes());
        dto.setCreatedAt(d.getCreatedAt());
        dto.setUpdatedAt(d.getUpdatedAt());
        return dto;
    }
}
