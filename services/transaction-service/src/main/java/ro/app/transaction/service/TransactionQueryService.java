package ro.app.transaction.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.app.transaction.model.entity.Transaction;
import ro.app.transaction.model.enums.TransactionType;
import ro.app.transaction.model.view.ViewTransaction;
import ro.app.transaction.repository.TransactionRepository;
import ro.app.transaction.repository.ViewTransactionRepository;

/**
 * Direct persistence access for transactions and the read-only view — no cross-service calls.
 */
@Service
public class TransactionQueryService {

    private final TransactionRepository transactionRepository;
    private final ViewTransactionRepository viewTransactionRepository;

    public TransactionQueryService(
            TransactionRepository transactionRepository,
            ViewTransactionRepository viewTransactionRepository) {
        this.transactionRepository = transactionRepository;
        this.viewTransactionRepository = viewTransactionRepository;
    }

    public List<ViewTransaction> getAllView() {
        return viewTransactionRepository.findAll();
    }

    public List<Transaction> getTransactionsByAccountId(Long accountId) {
        return transactionRepository.findByAccountIdOrderByTransactionDateDesc(accountId);
    }

    public List<Transaction> getTransactionsByAccountIds(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return List.of();
        }
        return transactionRepository.findByAccountIdIn(accountIds);
    }

    public List<Transaction> getTransactionsBetweenDates(LocalDate from, LocalDate to) {
        return transactionRepository.findBetweenDates(from, to);
    }

    public List<Transaction> getTransactionsByType(String code) {
        TransactionType type = TransactionType.fromCode(code);
        return transactionRepository.findByTransactionType(type);
    }

    public Map<LocalDate, BigDecimal> calculateDailyTotals() {
        Map<LocalDate, BigDecimal> result = new HashMap<>();
        for (Object[] row : transactionRepository.calculateDailyTotals()) {
            LocalDate day = ((java.sql.Date) row[0]).toLocalDate();
            BigDecimal total = (BigDecimal) row[1];
            result.put(day, total);
        }
        return result;
    }

    public List<Transaction> getFlaggedTransactions() {
        return transactionRepository.findByFlaggedTrueOrderByTransactionDateDesc();
    }

    public Transaction getById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ro.app.transaction.exception.ResourceNotFoundException(
                        "Transaction not found with id: " + id));
    }

    public Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    /**
     * GDPR: replace transaction narrative for all rows tied to the given accounts.
     */
    @Transactional
    public int anonymizeDetailsForAccountIds(List<Long> accountIds, String replacement) {
        if (accountIds == null || accountIds.isEmpty()) {
            return 0;
        }
        return transactionRepository.anonymizeDetailsForAccountIds(replacement, accountIds);
    }
}
