package ro.app.backend_Java_SpringBoot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ro.app.backend_Java_SpringBoot.model.TransactionTable;

public interface TransactionRepository extends JpaRepository<TransactionTable, Long> {
    // Toate tranzacțiile unui cont
    @Query("SELECT t FROM TransactionTable t WHERE t.account.iban = :iban ORDER BY t.transactionDate DESC")
    List<TransactionTable> findByAccountIban(@Param("iban") String iban);

    // Toate pt client
    @Query("SELECT t FROM TransactionTable t WHERE t.account.client.id = :clientId ORDER BY t.transactionDate DESC")
    List<TransactionTable> findByClientId(@Param("clientId") Long clientId);

    // Tranzacțiile într-un interval de date — comparăm doar partea de dată a LocalDateTime
    @Query("SELECT t FROM TransactionTable t WHERE function('date', t.transactionDate) BETWEEN :from AND :to ORDER BY t.transactionDate")
    List<TransactionTable> findBetweenDates(@Param("from") java.time.LocalDate from, @Param("to") java.time.LocalDate to);

    // Tranzacțiile de un anumit tip 
    @Query("SELECT t FROM TransactionTable t WHERE t.transactionType.code = :code ORDER BY t.transactionDate DESC")
    List<TransactionTable> findByTransactionTypeCode(@Param("code") String code);

    @Query(value = """
        SELECT DATE(t.transaction_date) AS day,
               SUM(CASE WHEN t.sign = '+' THEN t.amount ELSE -t.amount END) AS total
        FROM tranzactii t
        GROUP BY DATE(t.transaction_date)
        ORDER BY DATE(t.transaction_date)
    """, nativeQuery = true)
    List<Object[]> getDailyTotals();
}
