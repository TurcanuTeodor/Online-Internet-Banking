package ro.app.transaction.service;

import org.springframework.stereotype.Service;

import ro.app.transaction.model.entity.Transaction;
import ro.app.transaction.model.enums.TransactionType;
import ro.app.transaction.model.view.ViewTransaction;
import ro.app.transaction.repository.TransactionRepository;
import ro.app.transaction.repository.ViewTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ViewTransactionRepository viewTransactionRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              ViewTransactionRepository viewTransactionRepository) {
        this.transactionRepository = transactionRepository;
        this.viewTransactionRepository = viewTransactionRepository;
    }

    // 1) View-only list (from VIEW_TRANSACTION)
    public List<ViewTransaction> getAllView() {
        return viewTransactionRepository.findAll();
    }

    // 2) Transactions by account ID
    public List<Transaction> getTransactionsByAccountId(Long accountId) {
        return transactionRepository.findByAccountIdOrderByTransactionDateDesc(accountId);
    }

    // 3) Transactions for multiple accounts (used when caller provides account IDs for a client)
    public List<Transaction> getTransactionsByAccountIds(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return List.of();
        }
        return transactionRepository.findByAccountIdIn(accountIds);
    }

    // 4) Transactions between dates
    public List<Transaction> getTransactionsBetweenDates(LocalDate from, LocalDate to) {
        return transactionRepository.findBetweenDates(from, to);
    }

    // 5) Transactions by type (e.g. DEPOSIT, WITHDRAWAL, TRANSFER_INTERNAL, TRANSFER_EXTERNAL)
    public List<Transaction> getTransactionsByType(String code) {
        TransactionType type = TransactionType.fromCode(code);
        return transactionRepository.findByTransactionType(type);
    }

    // 6) Daily totals (aggregated report)
    public Map<LocalDate, BigDecimal> calculateDailyTotals() {
        Map<LocalDate, BigDecimal> result = new HashMap<>();
        for (Object[] row : transactionRepository.calculateDailyTotals()) {
            LocalDate day = ((java.sql.Date) row[0]).toLocalDate();
            BigDecimal total = (BigDecimal) row[1];
            result.put(day, total);
        }
        return result;
    }

    // 7) Flagged transactions
    public List<Transaction> getFlaggedTransactions() {
        return transactionRepository.findByFlaggedTrueOrderByTransactionDateDesc();
    }

    // 8) Find by ID
    public Transaction getById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ro.app.transaction.exception.ResourceNotFoundException(
                        "Transaction not found with id: " + id));
    }

    // 9) Save a new transaction
    public Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }
}
