package ro.app.backend_Java_SpringBoot.service;

import org.springframework.stereotype.Service;
import ro.app.backend_Java_SpringBoot.model.Transaction;
import ro.app.backend_Java_SpringBoot.model.ViewTransaction;
import ro.app.backend_Java_SpringBoot.repository.TransactionRepository;
import ro.app.backend_Java_SpringBoot.repository.ViewTransactionRepository;

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

    // 1) View-only list (from view_tranzactii)
    public List<ViewTransaction> getAllView() {
        return viewTransactionRepository.findAll();
    }

    // 2) Transactions by IBAN (real table)
    public List<Transaction> getTransactionsByAccountIban(String iban) {
        return transactionRepository.findByAccountIban(iban);
    }

    // 3) Transactions by client ID
    public List<Transaction> getTransactionsByClient(Long clientId) {
        return transactionRepository.findByClientId(clientId);
    }

    // 4) Transactions between dates
    public List<Transaction> getTransactionsBetweenDates(LocalDate from, LocalDate to) {
        return transactionRepository.findBetweenDates(from, to);
    }

    // 5) Transactions by type (e.g. TRANSFER, DEPOSIT)
    public List<Transaction> getTransactionsByType(String code) {
        return transactionRepository.findByTransactionTypeCode(code);
    }

    // 6) Daily totals (example of aggregation)
    public Map<LocalDate, BigDecimal> calculateDailyTotals() {
        Map<LocalDate, BigDecimal> result = new HashMap<>();
        for (Object[] row : transactionRepository.calculateDailyTotals()) {
            LocalDate day = ((java.sql.Date) row[0]).toLocalDate();
            BigDecimal total = (BigDecimal) row[1];
            result.put(day, total);
        }
        return result;
    }

}
