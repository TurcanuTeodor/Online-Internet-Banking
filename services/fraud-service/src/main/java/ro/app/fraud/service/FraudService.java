package ro.app.fraud.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ro.app.fraud.client.AccountSecurityClient;
import ro.app.fraud.dto.FraudDecisionDTO;
import ro.app.fraud.dto.FraudEvaluationRequest;
import ro.app.fraud.dto.FraudEvaluationResponse;
import ro.app.fraud.model.entity.FraudDecision;
import ro.app.fraud.model.enums.FraudDecisionStatus;
import ro.app.fraud.model.enums.FraudTier;
import ro.app.fraud.model.enums.FraudUserResolution;
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
    private final AccountSecurityClient accountSecurityClient;

    public FraudService(FraudDecisionRepository decisionRepo,
                        UserBehaviorProfileRepository profileRepo,
                        RuleEngine ruleEngine,
                        Tier2AsyncRunner tier2Runner,
                        AccountSecurityClient accountSecurityClient) {
        this.decisionRepo = decisionRepo;
        this.profileRepo = profileRepo;
        this.ruleEngine = ruleEngine;
        this.tier2Runner = tier2Runner;
        this.accountSecurityClient = accountSecurityClient;
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
        decision.setTransactionId(req.getTransactionId());
        decision.setCorrelationId(req.getCorrelationId());
        decision.setStatus(tier1.status());
        decision.setDecidedByTier(FraudTier.TIER1_RULES);
        decision.setRiskScore(tier1.riskScore());
        decision.setRuleHits(tier1.ruleHits());
        decision.setExplanation(tier1.explanation());

        decision = decisionRepo.save(decision);

        if (tier1.status() == FraudDecisionStatus.MANUAL_REVIEW || tier1.status() == FraudDecisionStatus.ALLOW) {
            tier2Runner.run(decision.getId(), req);
        }

        FraudEvaluationResponse resp = new FraudEvaluationResponse();
        resp.setDecisionId(decision.getId());
        resp.setTransactionId(decision.getTransactionId());
        resp.setCorrelationId(decision.getCorrelationId());
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

    public FraudDecisionDTO getByCorrelationId(String correlationId) {
        FraudDecision d = decisionRepo.findByCorrelationId(correlationId)
                .orElseThrow(() -> new RuntimeException("No decision for correlationId: " + correlationId));
        return toDto(d);
    }

    public Page<FraudDecisionDTO> getAlerts(Pageable pageable) {
        Set<FraudDecisionStatus> alertStatuses = EnumSet.of(
                FraudDecisionStatus.FLAG,
                FraudDecisionStatus.BLOCK,
                FraudDecisionStatus.MANUAL_REVIEW
        );
        return decisionRepo.findByStatusInAndUserResolution(List.copyOf(alertStatuses), FraudUserResolution.PENDING, pageable)
            .map(this::toDto);
    }

    public Page<FraudDecisionDTO> getMyAlerts(Long clientId, Pageable pageable) {
        Set<FraudDecisionStatus> alertStatuses = EnumSet.of(
                FraudDecisionStatus.FLAG,
                FraudDecisionStatus.BLOCK,
                FraudDecisionStatus.MANUAL_REVIEW,
                FraudDecisionStatus.STEP_UP_REQUIRED
        );
        List<FraudDecisionDTO> results = decisionRepo.findByClientId(clientId).stream()
                .filter(d -> alertStatuses.contains(d.getStatus()))
                .sorted(Comparator.comparing(FraudDecision::getCreatedAt).reversed())
                .map(this::toDto)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        if (start >= results.size()) {
            return new PageImpl<>(List.of(), pageable, results.size());
        }
        int end = Math.min(start + pageable.getPageSize(), results.size());
        return new PageImpl<>(results.subList(start, end), pageable, results.size());
    }

    public FraudDecisionDTO resolveMyAlert(Long decisionId, Long clientId, FraudUserResolution resolution, String notes) {
        FraudDecision d = decisionRepo.findById(decisionId)
                .orElseThrow(() -> new RuntimeException("Decision not found: " + decisionId));

        if (!clientId.equals(d.getClientId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot resolve another client's alert");
        }

        if (resolution == null || resolution == FraudUserResolution.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resolution is required");
        }

        d.setUserResolution(resolution);
        d.setUserResolutionNotes(notes);
        d.setUserResolvedAt(LocalDateTime.now());
        applyAccountStatusChange(resolution, d.getAccountId());

        d = decisionRepo.save(d);
        return toDto(d);
    }

    private void applyAccountStatusChange(FraudUserResolution resolution, Long accountId) {
        if (resolution == FraudUserResolution.LEGITIMATE) {
            try {
                accountSecurityClient.unfreezeAccount(accountId);
            } catch (Exception e) {
                log.warn("Failed to unfreeze account {} after legitimate resolution: {}", accountId, e.getMessage());
            }
        } else if (resolution == FraudUserResolution.FRAUD_REPORTED) {
            try {
                accountSecurityClient.freezeAccount(accountId);
            } catch (Exception e) {
                log.warn("Failed to freeze account {} after fraud report: {}", accountId, e.getMessage());
            }
        }
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

    /**
     * Maps a FraudDecision entity to its DTO.
     * Manual mapping is intentional — MapStruct is not on the classpath and
     * all fields are explicitly listed so any new field added to the entity
     * is immediately visible as a compilation gap here.
     */
    private FraudDecisionDTO toDto(FraudDecision d) {
        FraudDecisionDTO dto = new FraudDecisionDTO();
        dto.setId(d.getId());
        dto.setTransactionId(d.getTransactionId());
        dto.setCorrelationId(d.getCorrelationId());
        dto.setAccountId(d.getAccountId());
        dto.setClientId(d.getClientId());
        dto.setStatus(d.getStatus());
        dto.setDecidedByTier(d.getDecidedByTier());
        dto.setRiskScore(d.getRiskScore());
        dto.setRuleHits(d.getRuleHits());
        dto.setExplanation(d.getExplanation());
        dto.setReviewedByAdmin(d.getReviewedByAdmin());
        dto.setAdminNotes(d.getAdminNotes());
        dto.setUserResolution(d.getUserResolution());
        dto.setUserResolutionNotes(d.getUserResolutionNotes());
        dto.setUserResolvedAt(d.getUserResolvedAt());
        dto.setCreatedAt(d.getCreatedAt());
        dto.setUpdatedAt(d.getUpdatedAt());
        return dto;
    }
}
