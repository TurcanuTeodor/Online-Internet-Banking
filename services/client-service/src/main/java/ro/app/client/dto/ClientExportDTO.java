package ro.app.client.dto;

import java.time.LocalDateTime;

//GDPR Data Export DTO
public class ClientExportDTO {
    // === Date client ===
    private Long clientId;
    private String firstName;
    private String lastName;
    private String clientType;
    private String sexType;
    private String riskLevel;
    private Boolean active;
    private LocalDateTime accountCreatedAt;

    // === Date contact ===
    private String email;
    private String phone;
    private String contactPerson;
    private String website;
    private String address;
    private String city;
    private String postalCode;

    // === Metadata export ===
    private LocalDateTime exportedAt;
    private String exportReason;

    public ClientExportDTO() {
        this.exportedAt = LocalDateTime.now();
        this.exportReason = "GDPR data export request";
    }

    // === Getters & Setters ===

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getClientType() { return clientType; }
    public void setClientType(String clientType) { this.clientType = clientType; }

    public String getSexType() { return sexType; }
    public void setSexType(String sexType) { this.sexType = sexType; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getAccountCreatedAt() { return accountCreatedAt; }
    public void setAccountCreatedAt(LocalDateTime accountCreatedAt) { this.accountCreatedAt = accountCreatedAt; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public LocalDateTime getExportedAt() { return exportedAt; }
    public void setExportedAt(LocalDateTime exportedAt) { this.exportedAt = exportedAt; }

    public String getExportReason() { return exportReason; }
    public void setExportReason(String exportReason) { this.exportReason = exportReason; }
}
