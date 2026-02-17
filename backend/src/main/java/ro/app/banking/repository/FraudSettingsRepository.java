package ro.app.banking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ro.app.banking.model.entity.FraudSettings;

import java.util.Optional;

@Repository
public interface FraudSettingsRepository extends JpaRepository<FraudSettings, Long> {
    Optional<FraudSettings> findBySettingKey(String settingKey);
}
