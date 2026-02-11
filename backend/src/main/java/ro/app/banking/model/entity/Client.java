package ro.app.banking.model.entity;

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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import ro.app.banking.model.enums.ClientType;
import ro.app.banking.model.enums.SexType;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "\"CLIENT\"")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "client_type", nullable = false, columnDefinition = "CLIENT_TYPE_ENUM")
    private ClientType clientType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "sex_type", nullable = false, columnDefinition = "SEX_TYPE_ENUM")
    private SexType sexType;

    @OneToMany(mappedBy = "client", cascade = CascadeType.PERSIST)
    @JsonManagedReference("client-accounts")
    @JsonIgnore // prevent serialization cycle and avoid accidentally exposing accounts
    private List<Account> accounts = new ArrayList<>();

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
        return sexType;
    }

    public void setSex(SexType sex) {
        this.sexType = sex;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // === METODE UTILE
    public void addAccount(Account account) {
        accounts.add(account);
        account.setClient(this);
    }

    public void removeAccount(Account account) {
        accounts.remove(account);
        account.setClient(null);
    }
}