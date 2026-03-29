package ro.app.fraud.model.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import ro.app.fraud.model.enums.FraudDecisionStatus;
import ro.app.fraud.model.enums.FraudTier;

@Entity
@Table(name = "FRAUD_DECISION", schema = "fraud")
public class FraudDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TRANSACTION_ID")
    private Long transactionId;

    @Column(name = "ACCOUNT_ID", nullable = false)
    private Long accountId;

    @Column(name = "CLIENT_ID", nullable = false)
    private Long clientId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "STATUS", nullable = false, columnDefinition = "FRAUD_DECISION_STATUS_ENUM")
    private FraudDecisionStatus status = FraudDecisionStatus.ALLOW;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "DECIDED_BY_TIER", nullable = false, columnDefinition = "FRAUD_TIER_ENUM")
    private FraudTier decidedByTier = FraudTier.TIER1_RULES;

    @Column(name = "RISK_SCORE", nullable = false)
    private double riskScore;

    @Column(name = "RULE_HITS")
    private String ruleHits;

    @Column(name = "EXPLANATION")
    private String explanation;

    @Column(name = "REVIEWED_BY_ADMIN")
    private String reviewedByAdmin;

    @Column(name = "ADMIN_NOTES")
    private String adminNotes;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
