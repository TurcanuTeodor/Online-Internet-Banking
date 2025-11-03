package ro.app.backend_Java_SpringBoot.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;

@Entity
@Table(name = "date_de_contact")
public class ContactInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private ClientTable client;

    @Column(name = "telefon", length = 20)
    private String phone;

    @Email
    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "persoana_contact", length = 100)
    private String contactPerson;

    @Column(name = "site_web", length = 100)
    private String website;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ClientTable getClient() { return client; }
    public void setClient(ClientTable client) { this.client = client; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
}
