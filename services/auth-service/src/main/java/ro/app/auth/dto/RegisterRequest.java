package ro.app.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    // Optional: if provided, link to existing client; otherwise create new client
    private Long clientId;
    
    @NotBlank(message = "Username/Email is required")
    private String usernameOrEmail;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // Fields for creating new client (required if clientId is null)
    private String firstName;
    private String lastName;
    private String sexCode; // M, F, O
    private String clientTypeCode; // PF (personal), PJ (business)

    public RegisterRequest(){ }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getUsernameOrEmail() { return usernameOrEmail; }
    public void setUsernameOrEmail(String usernameOrEmail) { this.usernameOrEmail = usernameOrEmail; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getSexCode() { return sexCode; }
    public void setSexCode(String sexCode) { this.sexCode = sexCode; }

    public String getClientTypeCode() { return clientTypeCode; }
    public void setClientTypeCode(String clientTypeCode) { this.clientTypeCode = clientTypeCode; }
}
