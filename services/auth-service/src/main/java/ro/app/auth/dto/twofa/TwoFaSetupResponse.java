package ro.app.auth.dto.twofa;

public class TwoFaSetupResponse {
    private String otpauthUrl;
    private String secret;

    public TwoFaSetupResponse() {}

    public TwoFaSetupResponse(String otpauthUrl, String secret) {
        this.otpauthUrl = otpauthUrl;
        this.secret = secret;
    }

    public String getOtpauthUrl() { return otpauthUrl; }
    public String getSecret() { return secret; }
}
