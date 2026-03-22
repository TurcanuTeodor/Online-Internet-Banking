package ro.app.client.model.view;

import org.hibernate.annotations.Immutable;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "\"VIEW_CLIENT\"")
public class ViewClient {

    @Id
    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "client_last_name")
    private String clientLastName;

    @Column(name = "client_first_name")
    private String clientFirstName;

    @Column(name = "client_type_name")
    private String clientTypeName;

    @Column(name = "risk_level")
    private String riskLevel;

    @Column(name = "client_active")
    private Boolean active;

    @Column(name = "client_created_at")
    private LocalDateTime createdAt;

    // Aceste câmpuri sunt criptate AES-256 în DB — NU expune direct în response
    @Column(name = "email_encrypted")
    private String emailEncrypted;

    @Column(name = "phone_encrypted")
    private String phoneEncrypted;

    @Column(name = "address_encrypted")
    private String addressEncrypted;

    @Column(name = "city_encrypted")
    private String cityEncrypted;

    @Column(name = "postal_code_encrypted")
    private String postalCodeEncrypted;

    // Getters
    public Long getClientId() { return clientId; }
    public String getClientLastName() { return clientLastName; }
    public String getClientFirstName() { return clientFirstName; }
    public String getClientTypeName() { return clientTypeName; }
    public String getRiskLevel() { return riskLevel; }
    public Boolean getActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getEmailEncrypted() { return emailEncrypted; }
    public String getPhoneEncrypted() { return phoneEncrypted; }
    public String getAddressEncrypted() { return addressEncrypted; }
    public String getCityEncrypted() { return cityEncrypted; }
    public String getPostalCodeEncrypted() { return postalCodeEncrypted; }
}