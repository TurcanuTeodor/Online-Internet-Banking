package ro.app.transaction.model.enums;

public enum TransactionType {
    DEP("Deposit"),
    WDL("Withdrawal"),
    TR_INT("Internal Transfer"),
    TR_EXT("External Transfer");

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
            return TR_INT;
        }
        String c = code.trim().toUpperCase();
        
        // Legacy fallbacks for backward compatibility
        if (c.equals("DEPOSIT")) return DEP;
        if (c.equals("WITHDRAWAL")) return WDL;
        if (c.equals("TRANSFER_INTERNAL")) return TR_INT;
        if (c.equals("TRANSFER_EXTERNAL")) return TR_EXT;

        try {
            return TransactionType.valueOf(c);
        } catch (IllegalArgumentException e) {
            return TR_INT;
        }
    }
}
