package ro.app.transaction.service;

import java.util.List;

import org.springframework.stereotype.Service;

import ro.app.transaction.client.AccountRestClient;
import ro.app.transaction.client.ExternalAccountDto;
import ro.app.transaction.model.entity.Transaction;
import ro.app.transaction.repository.TransactionRepository;

/**
 * Flows that call account-service via {@link AccountRestClient} before reading from {@link TransactionRepository}.
 */
@Service
public class TransactionOrchestrationService {

    private final AccountRestClient accountRestClient;
    private final TransactionRepository transactionRepository;

    public TransactionOrchestrationService(
            AccountRestClient accountRestClient,
            TransactionRepository transactionRepository) {
        this.accountRestClient = accountRestClient;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Resolves all account IDs for a client via account-service, then loads transactions for those accounts.
     */
    public List<Transaction> getTransactionsForClientViaAccounts(Long clientId, String authorizationHeader) {
        List<ExternalAccountDto> accounts = accountRestClient.getAccountsByClient(clientId, authorizationHeader);
        if (accounts.isEmpty()) {
            return List.of();
        }
        List<Long> accountIds = accounts.stream().map(ExternalAccountDto::getId).toList();
        return transactionRepository.findByAccountIdIn(accountIds);
    }

    /**
     * Account metadata by IBAN (account-service enforces JWT; transaction-controller adds {@code OwnershipChecker}).
     */
    public ExternalAccountDto getAccountSummaryForIban(String iban, String authorizationHeader) {
        return accountRestClient.getAccountByIban(iban, authorizationHeader);
    }
}
