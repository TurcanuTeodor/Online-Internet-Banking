package ro.app.banking.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ClientDTO {
    private Long id;

    @NotBlank
    private String lastName;

    private String firstName;

    @NotNull
    private Long clientTypeId;

    @NotNull
    private Long sexId;

    @NotNull    
    private boolean active = true;

    private List<Long> accountIds = new ArrayList<>();

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public Long getClientTypeId() { return clientTypeId; }
    public void setClientTypeId(Long clientTypeId) { this.clientTypeId = clientTypeId; }
    public Long getSexId() { return sexId; }
    public void setSexId(Long sexId) { this.sexId = sexId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<Long> getAccountIds() { return accountIds; }
    public void setAccountIds(List<Long> accountIds) { this.accountIds = accountIds; }
}
