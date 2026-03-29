package ro.app.auth.dto.token;

public class RefreshTokenRequest {
    private String refreshToken;
    /**
     * Previous access JWT (may be expired). Used to copy the {@code ek} claim into the new access token
     * so client-service can keep decrypting PII without re-entering the password.
     */
    private String accessToken;

    public RefreshTokenRequest() {}

    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
