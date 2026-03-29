package ro.app.fraud.tier1;

import ro.app.fraud.model.enums.FraudDecisionStatus;

public record RuleResult(
        FraudDecisionStatus status,
        double riskScore,
        String ruleHits,
        String explanation
) {
    public static RuleResult allow() {
        return new RuleResult(FraudDecisionStatus.ALLOW, 0.0, "none", "No rules triggered — transaction is clean");
    }

    public static RuleResult review(String ruleHits, String explanation) {
        return new RuleResult(FraudDecisionStatus.MANUAL_REVIEW, 25.0, ruleHits, explanation);
    }

    public static RuleResult block(double riskScore, String ruleHits, String explanation) {
        return new RuleResult(FraudDecisionStatus.BLOCK, riskScore, ruleHits, explanation);
    }
}
