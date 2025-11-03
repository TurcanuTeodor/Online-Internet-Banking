package ro.app.backend_Java_SpringBoot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ro.app.backend_Java_SpringBoot.model.CurrencyType;
import java.util.Optional;

@Repository
public interface CurrencyTypeRepository extends JpaRepository<CurrencyType, Long> {
    Optional<CurrencyType> findByCodeIgnoreCase(String code);
}
