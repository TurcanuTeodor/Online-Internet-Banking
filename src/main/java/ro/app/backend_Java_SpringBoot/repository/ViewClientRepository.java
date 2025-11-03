package ro.app.backend_Java_SpringBoot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.app.backend_Java_SpringBoot.model.ViewClientTable;

public interface ViewClientRepository extends JpaRepository<ViewClientTable, Long> {

}