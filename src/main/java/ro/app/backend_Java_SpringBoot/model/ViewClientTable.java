package ro.app.backend_Java_SpringBoot.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

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
}
