package ro.app.banking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ro.app.banking.model.entity.FraudScore;
import ro.app.banking.model.entity.Transaction;

import java.util.List;
import java.util.Optional;

@Repository
public interface FraudScoreRepository extends JpaRepository<FraudScore, Long> {
    Optional<FraudScore> findByTransaction(Transaction transaction);
    
    @Query("SELECT fs FROM FraudScore fs WHERE fs.riskLevel = :riskLevel ORDER BY fs.createdAt DESC")
    List<FraudScore> findByRiskLevel(@Param("riskLevel") String riskLevel);
    
    @Query("SELECT fs FROM FraudScore fs WHERE fs.transaction.account.client.id = :clientId ORDER BY fs.createdAt DESC")
    List<FraudScore> findByClientId(@Param("clientId") Long clientId);
}
