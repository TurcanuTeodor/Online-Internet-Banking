package ro.app.banking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ro.app.banking.model.CurrencyType;
import java.util.Optional;

@Repository
public interface CurrencyTypeRepository extends JpaRepository<CurrencyType, Long> {
    Optional<CurrencyType> findByCodeIgnoreCase(String code);
}
