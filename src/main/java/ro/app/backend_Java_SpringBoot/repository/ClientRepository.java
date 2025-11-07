package ro.app.backend_Java_SpringBoot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.app.backend_Java_SpringBoot.model.ClientTable;
import java.util.List;

public interface ClientRepository extends JpaRepository<ClientTable, Long> {
    
    List<ClientTable> findByLastNameContainingIgnoreCase(String name);

    // adăugat: căutare după last OR first name
    List<ClientTable> findByLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCase(String lastName, String firstName);
}