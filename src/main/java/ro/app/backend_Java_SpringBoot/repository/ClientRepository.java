package ro.app.backend_Java_SpringBoot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.app.backend_Java_SpringBoot.model.Client;

public interface ClientRepository extends JpaRepository<Client, Long> {
    
    List<Client> findByLastNameContainingIgnoreCase(String name);

    List<Client> findByLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCase(String name, String nameAgain);
}