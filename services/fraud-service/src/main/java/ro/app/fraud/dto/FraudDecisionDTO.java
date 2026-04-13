package ro.app.fraud.dto;

import java.time.LocalDateTime;

import ro.app.fraud.model.enums.FraudDecisionStatus;
import ro.app.fraud.model.enums.FraudTier;
import ro.app.fraud.model.enums.FraudUserResolution;

public class FraudDecisionDTO {

    private Long id;
    private Long transactionId;
    private Long accountId;
    private Long clientId;
    private FraudDecisionStatus status;
    private FraudTier decidedByTier;
    private double riskScore;
    private String ruleHits;
    private String explanation;
    private String reviewedByAdmin;
    private String adminNotes;
    private FraudUserResolution userResolution;
    private String userResolutionNotes;
    private LocalDateTime userResolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public FraudDecisionStatus getStatus() { return status; }
    public void setStatus(FraudDecisionStatus status) { this.status = status; }

    public FraudTier getDecidedByTier() { return decidedByTier; }
    public void setDecidedByTier(FraudTier decidedByTier) { this.decidedByTier = decidedByTier; }

    public double getRiskScore() { return riskScore; }
    public void setRiskScore(double riskScore) { this.riskScore = riskScore; }

    public String getRuleHits() { return ruleHits; }
    public void setRuleHits(String ruleHits) { this.ruleHits = ruleHits; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getReviewedByAdmin() { return reviewedByAdmin; }
    public void setReviewedByAdmin(String reviewedByAdmin) { this.reviewedByAdmin = reviewedByAdmin; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public FraudUserResolution getUserResolution() { return userResolution; }
    public void setUserResolution(FraudUserResolution userResolution) { this.userResolution = userResolution; }

    public String getUserResolutionNotes() { return userResolutionNotes; }
    public void setUserResolutionNotes(String userResolutionNotes) { this.userResolutionNotes = userResolutionNotes; }

    public LocalDateTime getUserResolvedAt() { return userResolvedAt; }
    public void setUserResolvedAt(LocalDateTime userResolvedAt) { this.userResolvedAt = userResolvedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
