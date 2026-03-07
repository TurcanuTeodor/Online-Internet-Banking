package ro.app.payment.model.enums;

public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED;

    public String getCode() {
        return name();
    }

    public static PaymentStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return PENDING;
        }
        try {
            return PaymentStatus.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PENDING;
        }
    }
}
