package ro.app.banking.dto.auth;

public class TwoFaConfirmRequest {
    private String code;

    public TwoFaConfirmRequest() {}

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
