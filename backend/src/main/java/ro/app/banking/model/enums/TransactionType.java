package ro.app.banking.model.enums;

public enum TransactionType {
    DEP("Deposit"),
    RET("Withdrawal"),
    TRF("Transfer");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    public static TransactionType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Transaction type code is required");
        }
        return TransactionType.valueOf(code.trim().toUpperCase());
    }
}
