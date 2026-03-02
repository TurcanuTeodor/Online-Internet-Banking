package ro.app.client.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ClientDTO {
    private Long id;

    @NotBlank
    private String lastName;

    @NotBlank
    private String firstName;

    @NotBlank
    private String clientTypeCode;

    @NotBlank
    private String sexCode;

    @NotNull    
    private boolean active = true;

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getClientTypeCode() { return clientTypeCode; }
    public void setClientTypeCode(String clientTypeCode) { this.clientTypeCode = clientTypeCode; }
    public String getSexCode() { return sexCode; }
    public void setSexCode(String sexCode) { this.sexCode = sexCode; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
