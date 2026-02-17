package ro.app.banking.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.math.RoundingMode;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.constraints.NotNull;
import ro.app.banking.exception.ResourceNotFoundException;
import ro.app.banking.model.entity.Account;
import ro.app.banking.model.entity.Client;
import ro.app.banking.model.entity.Transaction;
import ro.app.banking.model.enums.AccountStatus;
import ro.app.banking.model.enums.CurrencyType;
import ro.app.banking.model.enums.TransactionType;
import ro.app.banking.repository.AccountRepository;
import ro.app.banking.repository.ClientRepository;
import ro.app.banking.repository.TransactionRepository;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final ClientRepository clientRepository;
    private final TransactionRepository transactionRepository;
    private final IbanService ibanService;
    private final ExchangeRateService exchangeRateService;
    
    public AccountService(AccountRepository accountRepository,
                          ClientRepository clientRepository,
                          TransactionRepository transactionRepository,
                          IbanService ibanService,
                          ExchangeRateService exchangeRateService) {
        this.accountRepository = accountRepository;
        this.clientRepository = clientRepository;
        this.transactionRepository = transactionRepository;
        this.ibanService = ibanService;
        this.exchangeRateService = exchangeRateService;
    }

    // 1)Open a new account
    @Transactional
    @CacheEvict(value= "accountsByClient", key= "#clientId")
    public Account openAccount(@NotNull Long clientId, String currencyCode) {
        if (clientId == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }   

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        CurrencyType currency = CurrencyType.fromCode(currencyCode);

        Account account = new Account();
        account.setClient(client);
        account.setCurrency(currency);
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);

        // Generate unique IBAN using IbanService
        String iban = ibanService.generateIban(accountRepository::existsByIban);
        account.setIban(iban);

        return accountRepository.save(account);
    }

    // 2️) Close an existing account
    @Transactional
    @Caching(evict = {
            @CacheEvict(value= "accountsByClient", key= "#result.client.id"),
            @CacheEvict(value= "balance", key= "#result.iban")
    })
    public Account closeAccount(@NotNull Long id) {
        if(id==null){
            throw new IllegalArgumentException("Account ID cannot be null");
        }

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (AccountStatus.CLOSED.equals(account.getStatus())) {
            throw new IllegalStateException("Account already closed");
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Account balance must be zero before closing");
        }

        account.setStatus(AccountStatus.CLOSED);
        return accountRepository.save(account);
    }

    // 3️) Get all accounts for a specific client
    @Cacheable(value= "accountsByClient", key= "#clientId")
    public List<Account> getAccountsByClient(Long clientId) {
        return accountRepository.findByClientId(clientId);
    }

    // 4️) Get balance by IBAN
    @Cacheable(value= "balance", key="#iban")
    public BigDecimal getBalanceByIban(String iban) {
        return accountRepository.findByIban(iban)
                .map(Account::getBalance)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }

    // 5️) Transfer between two accounts
    @Transactional
    @Caching(evict = {
        @CacheEvict(value= "balance", key= "#fromIban"),
        @CacheEvict(value= "balance", key= "#toIban")
    })
    public void transfer(String fromIban, String toIban, BigDecimal amount) {
        if(amount ==null || amount.compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (fromIban.equalsIgnoreCase(toIban)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        Account from = accountRepository.findByIban(fromIban)
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));
        Account to = accountRepository.findByIban(toIban)
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));

        if (from.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        TransactionType transferType = TransactionType.TRANSFER_INTERNAL;

        CurrencyType fromCurrency = from.getCurrency();
        CurrencyType toCurrency = to.getCurrency();
        BigDecimal convertedAmount = amount;
        if (fromCurrency != toCurrency) {
            BigDecimal rate = exchangeRateService.getRate(fromCurrency, toCurrency);
            convertedAmount = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        }

        // update balances
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(convertedAmount));

        // debit transaction
        Transaction debit = new Transaction();
        debit.setAccount(from);
        debit.setAmount(amount);
        debit.setOriginalAmount(amount);
        debit.setOriginalCurrency(fromCurrency);
        debit.setSign("-");
        debit.setTransactionType(transferType);
        debit.setTransactionDate(LocalDateTime.now());
        debit.setDetails("Transfer to account " + toIban);

        // credit transaction
        Transaction credit = new Transaction();
        credit.setAccount(to);
        credit.setAmount(convertedAmount);
        credit.setOriginalAmount(amount);
        credit.setOriginalCurrency(fromCurrency);
        credit.setSign("+");
        credit.setTransactionType(transferType);
        credit.setTransactionDate(LocalDateTime.now());
        credit.setDetails("Transfer from account " + fromIban);

        transactionRepository.save(debit);
        transactionRepository.save(credit);
        accountRepository.save(from);
        accountRepository.save(to);
    }
}
