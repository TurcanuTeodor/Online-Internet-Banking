package ro.app.backend_Java_SpringBoot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.app.backend_Java_SpringBoot.model.ClientTable;

public interface ClientRepository extends JpaRepository<ClientTable, Long> {
    
    List<ClientTable> findByLastNameContainingIgnoreCase(String name);

    List<ClientTable> findByLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCase(String name, String nameAgain);
}