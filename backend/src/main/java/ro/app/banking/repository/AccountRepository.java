package ro.app.banking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.app.banking.model.entity.Account;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByIban(String iban);

    List<Account> findByClientId(Long clientId);
}

