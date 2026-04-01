package ro.app.client.model.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ro.app.client.model.enums.ClientType;
import ro.app.client.model.enums.SexType;

@Entity
@Table(name = "\"CLIENT\"")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "last_name", length = 500, nullable = false)
    private String lastName;

    @Column(name = "first_name", length = 500, nullable = false)
    private String firstName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "client_type", nullable = false, columnDefinition = "CLIENT_TYPE_ENUM")
    private ClientType clientType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "sex_type", nullable = false, columnDefinition = "SEX_TYPE_ENUM")
    private SexType sexType;

    @Column(name = "risk_level", length = 50, nullable = false)
    private String riskLevel = "LOW";

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public Client() {
    }

    public Client(String firstName, String lastName, ClientType clientType, SexType sex, boolean active) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.clientType = clientType;
        this.sexType = sex;
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public ClientType getClientType() { return clientType; }
    public void setClientType(ClientType clientType) { this.clientType = clientType; }

    public SexType getSexType() { return sexType; }
    public void setSexType(SexType sexType) { this.sexType = sexType; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel != null ? riskLevel : "LOW"; }
}
