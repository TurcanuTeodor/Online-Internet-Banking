package ro.app.backend_Java_SpringBoot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ro.app.backend_Java_SpringBoot.model.TransactionType;

import java.util.Optional;

@Repository
public interface TransactionTypeRepository extends JpaRepository<TransactionType, Long> {

    Optional<TransactionType> findByCodeIgnoreCase(String code);
}
