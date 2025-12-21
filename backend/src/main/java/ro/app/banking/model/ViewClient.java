package ro.app.banking.model;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Immutable
@Table(name = "\"VIEW_CLIENT\"")
public class ViewClient {
    @Id
    @Column(name="client_id")
    private Long clientId;

    @Column(name="client_last_name")
    private String clientLastName;

    @Column(name="client_first_name")
    private String clientFirstName;

    @Column(name="client_type_name")
    private String clientTypeName;

    @Column(name="sex_type_name")
    private String sexDescription;

    @Column(name = "client_active")
    private Boolean active;

    public Long getClientId() { return clientId; }
    public String getClientLastName() { return clientLastName; }
    public String getClientFirstName() { return clientFirstName; }
    public String getClientTypeName() { return clientTypeName; }
    public String getSexDescription() { return sexDescription; }
    public Boolean getActive() { return active; }

    @Override
    public String toString() {
        return "ViewClient{" +
                "clientId=" + clientId +
                ", clientLastName='" + clientLastName + '\'' +
                ", clientFirstName='" + clientFirstName + '\'' +
                ", clientTypeName='" + clientTypeName + '\'' +
                ", sexDescription='" + sexDescription + '\'' +
                ", active=" + active +
                '}';
    }
}
