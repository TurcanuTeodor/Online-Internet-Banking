package ro.app.fraud.dto;

import ro.app.fraud.model.enums.FraudDecisionStatus;
import ro.app.fraud.model.enums.FraudTier;

public class FraudEvaluationResponse {

    private Long decisionId;
    private Long transactionId;
    private String correlationId;
    private FraudDecisionStatus status;
    private FraudTier decidedByTier;
    private double riskScore;
    private String ruleHits;
    private String explanation;

    public Long getDecisionId() { return decisionId; }
    public void setDecisionId(Long decisionId) { this.decisionId = decisionId; }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

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
}
