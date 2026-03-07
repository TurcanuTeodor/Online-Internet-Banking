package ro.app.client.model.enums;

public enum ClientType {
    PF("INDIVIDUAL"),
    PJ("COMPANY");

    private final String label;

    ClientType(String label) {
        this.label = label;
    }

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    public static ClientType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Client type code is required");
        }
        return ClientType.valueOf(code.trim().toUpperCase());
    }
}
