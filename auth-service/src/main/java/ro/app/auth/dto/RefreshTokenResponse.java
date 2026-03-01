package ro.app.auth.dto;

public class RefreshTokenResponse {
    private String token; // New access token
    private String refreshToken; // New refresh token (optional, for rotation)

    public RefreshTokenResponse(){}

    public RefreshTokenResponse(String token) {
        this.token = token;
    }

    public RefreshTokenResponse(String token, String refreshToken) {
        this.token = token;
        this.refreshToken = refreshToken;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
