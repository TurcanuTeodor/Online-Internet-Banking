package ro.app.fraud.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import ro.app.fraud.model.entity.FraudDecision;
import ro.app.fraud.model.enums.FraudDecisionStatus;
import ro.app.fraud.model.enums.FraudUserResolution;

public interface FraudDecisionRepository extends JpaRepository<FraudDecision, Long> {

    Optional<FraudDecision> findByTransactionId(Long transactionId);

    List<FraudDecision> findByClientId(Long clientId);

    Page<FraudDecision> findByStatusInAndUserResolution(List<FraudDecisionStatus> statuses, FraudUserResolution userResolution, Pageable pageable);

    Page<FraudDecision> findByStatus(FraudDecisionStatus status, Pageable pageable);

    Page<FraudDecision> findByStatusIn(List<FraudDecisionStatus> statuses, Pageable pageable);

    long countByStatus(FraudDecisionStatus status);

    long countByClientIdAndCreatedAtAfter(Long clientId, LocalDateTime after);
    
    long countByAccountIdAndCreatedAtAfter(Long accountId, LocalDateTime after);
}
