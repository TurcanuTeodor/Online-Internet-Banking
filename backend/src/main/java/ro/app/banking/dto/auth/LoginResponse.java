package ro.app.banking.dto.auth;

public class LoginResponse {
    private boolean twoFactorRequired;
    private String token; //final Jwt(after success login) or tempToken
    private Long clientId;
    private String role;

    public LoginResponse(){}

    public LoginResponse( boolean twoFactorRequired, String token,Long clientId, String role){
        this.twoFactorRequired= twoFactorRequired;
        this.token=token;
        this.clientId=clientId;
        this.role=role;
    }

    //only getters
    public boolean isTwoFactorRequired() { return twoFactorRequired; }
    public String getToken() { return token; }
    public Long getClientId() { return clientId; }
    public String getRole() { return role; }
}
