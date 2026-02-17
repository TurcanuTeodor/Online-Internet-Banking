package ro.app.banking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ro.app.banking.model.entity.FraudAlert;

import java.util.List;
import java.util.Optional;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {
    @Query("SELECT fa FROM FraudAlert fa WHERE fa.status = :status ORDER BY fa.createdAt DESC")
    List<FraudAlert> findByStatus(@Param("status") String status);
    
    @Query("SELECT fa FROM FraudAlert fa WHERE fa.fraudScore.transaction.account.client.id = :clientId ORDER BY fa.createdAt DESC")
    List<FraudAlert> findByClientId(@Param("clientId") Long clientId);
    
    @Query("SELECT COUNT(fa) FROM FraudAlert fa WHERE fa.status = 'PENDING'")
    long countPendingAlerts();
}
