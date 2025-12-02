package ro.app.banking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.app.banking.model.ViewAccount;

public interface ViewAccountRepository extends JpaRepository<ViewAccount, Long>{
    
}
