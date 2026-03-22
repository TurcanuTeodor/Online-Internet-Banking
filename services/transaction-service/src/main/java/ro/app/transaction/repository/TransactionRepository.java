package ro.app.transaction.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ro.app.transaction.model.entity.Transaction;
import ro.app.transaction.model.enums.TransactionType;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // All transactions for an account (by account_id — no cross-schema FK)
    List<Transaction> findByAccountIdOrderByTransactionDateDesc(Long accountId);

    // All transactions for multiple accounts (used for client lookup via account IDs)
    @Query("SELECT t FROM Transaction t WHERE t.accountId IN :accountIds ORDER BY t.transactionDate DESC")
    List<Transaction> findByAccountIdIn(@Param("accountIds") List<Long> accountIds);

    // Transactions between dates
    @Query("SELECT t FROM Transaction t WHERE function('date', t.transactionDate) BETWEEN :from AND :to ORDER BY t.transactionDate")
    List<Transaction> findBetweenDates(@Param("from") java.time.LocalDate from, @Param("to") java.time.LocalDate to);

    // Transactions by type
    @Query("SELECT t FROM Transaction t WHERE t.transactionType = :type ORDER BY t.transactionDate DESC")
    List<Transaction> findByTransactionType(@Param("type") TransactionType type);

    // Daily totals aggregation
    @Query("""
        SELECT function('date', t.transactionDate) AS day,
               SUM(CASE WHEN t.sign = '+' THEN t.amount ELSE -t.amount END) AS total
        FROM Transaction t
        GROUP BY function('date', t.transactionDate)
        ORDER BY function('date', t.transactionDate)
    """)
    List<Object[]> calculateDailyTotals();

    // Flagged transactions
    List<Transaction> findByFlaggedTrueOrderByTransactionDateDesc();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Transaction t SET t.details = :replacement WHERE t.accountId IN :accountIds")
    int anonymizeDetailsForAccountIds(
            @Param("replacement") String replacement,
            @Param("accountIds") List<Long> accountIds);
}
