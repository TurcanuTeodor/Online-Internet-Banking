package ro.app.backend_Java_SpringBoot.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "tip_tranzactie")
public class TransactionType {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "cod", nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "denumire", nullable = false, length = 100)
    private String name;

    // --- Getters & Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // --- Utility ---
    @Override
    public String toString() {
        return code + " - " + name;
    }
}
