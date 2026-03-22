package ro.app.transaction.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import ro.app.transaction.client.ExternalAccountDto;
import ro.app.transaction.model.entity.Transaction;
import ro.app.transaction.model.view.ViewTransaction;

/**
 * Facade — delegates to {@link TransactionQueryService} (repository-only) and
 * {@link TransactionOrchestrationService} (HTTP + repository). Public API unchanged for controllers.
 */
@Service
public class TransactionService {

    private final TransactionQueryService transactionQueryService;
    private final TransactionOrchestrationService transactionOrchestrationService;

    public TransactionService(
            TransactionQueryService transactionQueryService,
            TransactionOrchestrationService transactionOrchestrationService) {
        this.transactionQueryService = transactionQueryService;
        this.transactionOrchestrationService = transactionOrchestrationService;
    }

    public List<ViewTransaction> getAllView() {
        return transactionQueryService.getAllView();
    }

    public List<Transaction> getTransactionsByAccountId(Long accountId) {
        return transactionQueryService.getTransactionsByAccountId(accountId);
    }

    public List<Transaction> getTransactionsByAccountIds(List<Long> accountIds) {
        return transactionQueryService.getTransactionsByAccountIds(accountIds);
    }

    public List<Transaction> getTransactionsForClientViaAccounts(Long clientId, String authorizationHeader) {
        return transactionOrchestrationService.getTransactionsForClientViaAccounts(clientId, authorizationHeader);
    }

    public ExternalAccountDto getAccountSummaryForIban(String iban, String authorizationHeader) {
        return transactionOrchestrationService.getAccountSummaryForIban(iban, authorizationHeader);
    }

    public List<Transaction> getTransactionsBetweenDates(LocalDate from, LocalDate to) {
        return transactionQueryService.getTransactionsBetweenDates(from, to);
    }

    public List<Transaction> getTransactionsByType(String code) {
        return transactionQueryService.getTransactionsByType(code);
    }

    public Map<LocalDate, BigDecimal> calculateDailyTotals() {
        return transactionQueryService.calculateDailyTotals();
    }

    public List<Transaction> getFlaggedTransactions() {
        return transactionQueryService.getFlaggedTransactions();
    }

    public Transaction getById(Long id) {
        return transactionQueryService.getById(id);
    }

    public Transaction save(Transaction transaction) {
        return transactionQueryService.save(transaction);
    }
}
