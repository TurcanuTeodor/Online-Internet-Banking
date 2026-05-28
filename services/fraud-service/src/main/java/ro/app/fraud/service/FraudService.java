package ro.app.fraud.service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@Profile("!trainer") // nu se instantiaza in modul trainer (nu exista JPA/repository)
public class FraudService {

    private static final Logger log = LoggerFactory.getLogger(FraudService.class);

    private final FraudDecisionRepository decisionRepo;
    private final UserBehaviorProfileRepository profileRepo;
    private final RuleEngine ruleEngine;
    private final Tier2AsyncRunner tier2Runner;
    private final AccountSecurityClient accountSecurityClient;
    private final Tier2SyncEvaluator tier2SyncEvaluator;

    public FraudService(FraudDecisionRepository decisionRepo,
                        UserBehaviorProfileRepository profileRepo,
                        RuleEngine ruleEngine,
                        Tier2AsyncRunner tier2Runner,
                        AccountSecurityClient accountSecurityClient,
                        Tier2SyncEvaluator tier2SyncEvaluator) {
        this.decisionRepo = decisionRepo;
        this.profileRepo = profileRepo;
        this.ruleEngine = ruleEngine;
        this.tier2Runner = tier2Runner;
        this.accountSecurityClient = accountSecurityClient;
        this.tier2SyncEvaluator = tier2SyncEvaluator;
    }

    /**
     * Main entry point — called synchronously by account-service before committing a transfer.
     * Tier 1 runs synchronously. If MANUAL_REVIEW → async Tier 2 kicks off in background.
     */
    public FraudEvaluationResponse evaluate(FraudEvaluationRequest req) {
        log.info("Evaluating transfer: client={} amount={} {} -> {}",
                req.getClientId(), req.getAmount(),
                req.getSenderIban(), req.getReceiverIban());

        // -- TIER 1: synchronous, deterministic rules --
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
        decision.setAmount(req.getAmount());
        decision.setCurrencyCode(req.getCurrency());

        decision = decisionRepo.save(decision);

        // -- TIER 2: synchronous for ALLOW/MANUAL_REVIEW from Tier 1 --
        // Tier 1 already handles STEP_UP for hard rules (large amount etc.)
        // Tier 2 adds behavioral context synchronously so account-service
        // can enforce TOTP based on behavioral risk too.
        FraudDecisionStatus finalStatus = tier1.status();

        if (tier1.status() == FraudDecisionStatus.ALLOW
                || tier1.status() == FraudDecisionStatus.MANUAL_REVIEW) {

            FraudDecisionStatus tier2Status =
                    tier2SyncEvaluator.evaluate(decision.getId(), req);

            // Tier 2 can escalate: ALLOW → STEP_UP_REQUIRED or FLAG
            // Tier 2 cannot de-escalate Tier 1 STEP_UP_REQUIRED
            finalStatus = tier2Status;
            decision.setStatus(finalStatus);
            // Note: tier2SyncEvaluator already persisted its results
        }

        // -- TIER 3: async — for ML learning and post-hoc alerting --
        // Runs only when Tier 2 result is ALLOW (ambiguous cases already
        // handled: score 50-70 → STEP_UP, score 70+ → FLAG from Tier 2)
        // Tier 3 updates the decision record async but does NOT affect
        // what account-service receives synchronously.
        if (finalStatus == FraudDecisionStatus.ALLOW) {
            tier2Runner.runTier3Only(decision.getId(), req);
        }

        // -- BUILD RESPONSE --
        FraudEvaluationResponse resp = new FraudEvaluationResponse();
        resp.setDecisionId(decision.getId());
        resp.setTransactionId(decision.getTransactionId());
        resp.setCorrelationId(decision.getCorrelationId());
        resp.setStatus(finalStatus); 
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
                FraudDecisionStatus.MANUAL_REVIEW,
                FraudDecisionStatus.STEP_UP_REQUIRED
        );
        // Include both PENDING (unresolved) and FRAUD_REPORTED (user flagged as fraud)
        // so admin can review and take action on user-reported fraud cases.
        List<FraudUserResolution> visibleResolutions = List.of(
                FraudUserResolution.PENDING,
                FraudUserResolution.FRAUD_REPORTED
        );
        return decisionRepo.findByStatusInAndUserResolutionIn(
                List.copyOf(alertStatuses), visibleResolutions, pageable)
            .map(this::toDto);
    }

    /**
     * Returnează alertele unui client specific, paginate la nivel DB.
     *
     * Fix #8: Înlocuiește varianta anterioară care încărca TOATE deciziile în memorie
     * (și aplica filter+sort în Java). Acum:
     *   - Filtrarea statusurilor se face în SQL WHERE
     *   - Sortarea descrescătoare după createdAt se face în SQL ORDER BY
     *   - Paginarea (LIMIT/OFFSET) se face în SQL
     * Eliminat riscul de OOM pentru clienți cu istoric de mii de tranzacții.
     */
    public Page<FraudDecisionDTO> getMyAlerts(Long clientId, Pageable pageable) {
        Set<FraudDecisionStatus> alertStatuses = EnumSet.of(
                FraudDecisionStatus.FLAG,
                FraudDecisionStatus.BLOCK,
                FraudDecisionStatus.MANUAL_REVIEW,
                FraudDecisionStatus.STEP_UP_REQUIRED
        );

        // Sortare descrescătoare după createdAt impusă explicit la nivel DB
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("createdAt").descending()
        );

        return decisionRepo
                .findByClientIdAndStatusIn(clientId, List.copyOf(alertStatuses), sortedPageable)
                .map(this::toDto);
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
        dto.setAmount(d.getAmount());
        dto.setCurrencyCode(d.getCurrencyCode());
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
