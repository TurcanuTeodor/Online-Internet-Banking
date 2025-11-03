package ro.app.backend_Java_SpringBoot.model;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Immutable
@Table(name = "tip_client")
public class ClientType {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "cod", nullable = false)
    private String code;

    @Column(name = "denumire", nullable = false)
    private String description;

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
