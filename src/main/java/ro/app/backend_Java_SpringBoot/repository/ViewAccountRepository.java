package ro.app.backend_Java_SpringBoot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.app.backend_Java_SpringBoot.model.ViewAccountTable;

public interface ViewAccountRepository extends JpaRepository<ViewAccountTable, Long>{
    
}
