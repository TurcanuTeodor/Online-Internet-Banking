package ro.app.client.model.enums;

public enum SexType {
    M("MALE"),
    F("FEMALE"),
    O("OTHER");

    private final String label;

    SexType(String label) {
        this.label = label;
    }

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    public static SexType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Sex code is required");
        }
        return SexType.valueOf(code.trim().toUpperCase());
    }
}
