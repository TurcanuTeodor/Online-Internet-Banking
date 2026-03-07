package ro.app.transaction.model.enums;

public enum CurrencyType {
    EUR("Euro"),
    USD("US Dollar"),
    RON("Romanian Leu"),
    GBP("British Pound");

    private final String label;

    CurrencyType(String label) {
        this.label = label;
    }

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    public static CurrencyType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Currency code is required");
        }
        return CurrencyType.valueOf(code.trim().toUpperCase());
    }
}
