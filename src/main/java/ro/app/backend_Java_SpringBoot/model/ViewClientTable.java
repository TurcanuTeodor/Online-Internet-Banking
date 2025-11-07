package ro.app.backend_Java_SpringBoot.model;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Immutable
@Table(name = "view_client")
public class ViewClientTable {
    @Id
    @Column(name="client_id")
    private Long clientId;

    @Column(name="client_nume")
    private String clientLastName;

    @Column(name="client_prenume")
    private String clientFirstName;

    @Column(name="tip_client_denumire")
    private String clientTypeName;

    @Column(name="sex_denumire")
    private String sexDescription;

    @Column(name = "activ")
    private Boolean active;

    public Long getClientId() { return clientId; }
    public String getClientLastName() { return clientLastName; }
    public String getClientFirstName() { return clientFirstName; }
    public String getClientTypeName() { return clientTypeName; }
    public String getSexDescription() { return sexDescription; }
    public Boolean getActive() { return active; }

    @Override
    public String toString() {
        return "ViewClientTable{" +
                "clientId=" + clientId +
                ", clientLastName='" + clientLastName + '\'' +
                ", clientFirstName='" + clientFirstName + '\'' +
                ", clientTypeName='" + clientTypeName + '\'' +
                ", sexDescription='" + sexDescription + '\'' +
                ", active=" + active +
                '}';
    }
}
