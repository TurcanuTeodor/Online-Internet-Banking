package ro.app.banking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.app.banking.model.ViewClient;

public interface ViewClientRepository extends JpaRepository<ViewClient, Long> {

}