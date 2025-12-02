package ro.app.banking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ro.app.banking.model.TransactionType;

import java.util.Optional;

@Repository
public interface TransactionTypeRepository extends JpaRepository<TransactionType, Long> {

    Optional<TransactionType> findByCodeIgnoreCase(String code);
}
