package ro.app.banking.model.enums;

public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER_INTERNAL,
    TRANSFER_EXTERNAL;

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return name();
    }

    public static TransactionType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return TRANSFER_INTERNAL;
        }
        try {
            return TransactionType.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TRANSFER_INTERNAL;
        }
    }
}
