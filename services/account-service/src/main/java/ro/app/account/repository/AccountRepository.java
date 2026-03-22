package ro.app.account.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ro.app.account.model.entity.Account;
import ro.app.account.model.enums.AccountStatus;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByIban(String iban);

    List<Account> findByClientId(Long clientId);

    boolean existsByIban(String iban);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Account a SET a.status = :closed WHERE a.clientId = :clientId AND a.status <> :closed")
    int gdprCloseAllByClientId(@Param("clientId") Long clientId, @Param("closed") AccountStatus closed);
}
