package ro.app.banking.model.enums;

public enum TransactionCategory {
    FOOD(0.2, "Food & Dining"),
    GROCERIES(0.1, "Groceries"),
    TRANSPORT(0.15, "Transport"),
    SHOPPING(0.3, "Shopping"),
    ENTERTAINMENT(0.25, "Entertainment"),
    HEALTH(0.1, "Health & Medical"),
    TRAVEL(0.4, "Travel"),
    SUBSCRIPTIONS(0.15, "Subscriptions"),
    INCOME(0.0, "Income"),
    OTHERS(0.5, "Other");

    private final double baseRiskScore;
    private final String label;

    TransactionCategory(double baseRiskScore, String label) {
        this.baseRiskScore = baseRiskScore;
        this.label = label;
    }

    public double getBaseRiskScore() {
        return baseRiskScore;
    }

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    public static TransactionCategory fromCode(String code) {
        if (code == null || code.isBlank()) {
            return OTHERS;
        }
        try {
            return TransactionCategory.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHERS;
        }
    }
}
