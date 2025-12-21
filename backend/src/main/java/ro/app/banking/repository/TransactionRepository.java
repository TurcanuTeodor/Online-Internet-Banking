package ro.app.banking.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ro.app.banking.model.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Toate tranzacțiile unui cont
    @Query("SELECT t FROM Transaction t WHERE t.account.iban = :iban ORDER BY t.transactionDate DESC")
    List<Transaction> findByAccountIban(@Param("iban") String iban);

    // Toate pt client
    @Query("SELECT t FROM Transaction t WHERE t.account.client.id = :clientId ORDER BY t.transactionDate DESC")
    List<Transaction> findByClientId(@Param("clientId") Long clientId);

    // Tranzacțiile într-un interval de date — comparăm doar partea de dată a LocalDateTime
    @Query("SELECT t FROM Transaction t WHERE function('date', t.transactionDate) BETWEEN :from AND :to ORDER BY t.transactionDate")
    List<Transaction> findBetweenDates(@Param("from") java.time.LocalDate from, @Param("to") java.time.LocalDate to);

    // Tranzacțiile de un anumit tip 
    @Query("SELECT t FROM Transaction t WHERE t.transactionType.code = :code ORDER BY t.transactionDate DESC")
    List<Transaction> findByTransactionTypeCode(@Param("code") String code);

    @Query("""
        SELECT function('date', t.transactionDate) AS day,
               SUM(CASE WHEN t.sign = '+' THEN t.amount ELSE -t.amount END) AS total
        FROM Transaction t
        GROUP BY function('date', t.transactionDate)
        ORDER BY function('date', t.transactionDate)
    """)
    List<Object[]> calculateDailyTotals();
}
