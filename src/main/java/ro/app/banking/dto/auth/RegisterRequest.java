package ro.app.banking.dto.auth;

public class RegisterRequest {
    private Long clientId;
    private String usernameOrEmail;
    private String password;

    public RegisterRequest(){ }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getUsernameOrEmail() { return usernameOrEmail; }
    public void setUsernameOrEmail(String usernameOrEmail) { this.usernameOrEmail = usernameOrEmail; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; } 
}
