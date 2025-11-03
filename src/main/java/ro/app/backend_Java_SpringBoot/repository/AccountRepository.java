package ro.app.backend_Java_SpringBoot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.app.backend_Java_SpringBoot.model.AccountTable;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<AccountTable, Long> {
    Optional<AccountTable> findByIban(String iban);

    List<AccountTable> findByClientId(Long clientId);
}

