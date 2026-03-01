package ro.app.auth.dto;

public class LoginResponse {
    private boolean twoFactorRequired;
    private String token; //final Jwt(after success login) or tempToken
    private String refreshToken; // Refresh token for renewing access token
    private Long clientId;
    private String role;

    public LoginResponse(){}

    public LoginResponse(boolean twoFactorRequired, String token, String refreshToken, Long clientId, String role){
        this.twoFactorRequired= twoFactorRequired;
        this.token=token;
        this.refreshToken=refreshToken;
        this.clientId=clientId;
        this.role=role;
    }

    //only getters
    public boolean isTwoFactorRequired() { return twoFactorRequired; }
    public String getToken() { return token; }
    public String getRefreshToken() { return refreshToken; }
    public Long getClientId() { return clientId; }
    public String getRole() { return role; }
}

