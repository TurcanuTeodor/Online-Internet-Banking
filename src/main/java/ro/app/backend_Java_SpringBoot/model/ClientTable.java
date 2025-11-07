package ro.app.backend_Java_SpringBoot.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "client")
public class ClientTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nume", nullable = false)
    private String lastName;

    @Column(name = "prenume", nullable = false)
    private String firstName;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tip_client_id", nullable = false)
    private ClientType clientType;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sex_id", nullable = false)
    private SexType sex;

    @OneToMany(mappedBy = "client", cascade = CascadeType.PERSIST)
    @JsonManagedReference("client-accounts")
    @JsonIgnore // prevent serialization cycle and avoid accidentally exposing accounts
    private List<AccountTable> accounts = new ArrayList<>();

    @Column(name = "activ", nullable = false)
    private boolean active = true;


    public ClientTable() {
    }

    public ClientTable(String firstName, String lastName, ClientType clientType, SexType sex, boolean active) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.clientType = clientType;
        this.sex = sex;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public ClientType getClientType() {
        return clientType;
    }

    public void setClientType(ClientType clientType) {
        this.clientType = clientType;
    }

    public SexType getSex() {
        return sex;
    }

    public void setSex(SexType sex) {
        this.sex = sex;
    }

    public List<AccountTable> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<AccountTable> accounts) {
        this.accounts = accounts;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // === METODE UTILE
    public void addAccount(AccountTable account) {
        accounts.add(account);
        account.setClient(this);
    }

    public void removeAccount(AccountTable account) {
        accounts.remove(account);
        account.setClient(null);
    }
}