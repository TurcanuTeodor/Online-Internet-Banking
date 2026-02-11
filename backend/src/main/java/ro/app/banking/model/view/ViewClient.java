package ro.app.banking.model.view;

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

    @Column(name = "client_active")
    private Boolean active;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    public Long getClientId() { return clientId; }
    public String getClientLastName() { return clientLastName; }
    public String getClientFirstName() { return clientFirstName; }
    public String getClientTypeName() { return clientTypeName; }
    public Boolean getActive() { return active; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }

    @Override
    public String toString() {
        return "ViewClient{" +
                "clientId=" + clientId +
                ", clientLastName='" + clientLastName + '\'' +
                ", clientFirstName='" + clientFirstName + '\'' +
                ", clientTypeName='" + clientTypeName + '\'' +
                ", active=" + active +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
