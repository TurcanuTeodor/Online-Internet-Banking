package ro.app.banking.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.constraints.NotNull;
import ro.app.banking.exception.ResourceNotFoundException;
import ro.app.banking.model.Account;
import ro.app.banking.model.Client;
import ro.app.banking.model.CurrencyType;
import ro.app.banking.model.Transaction;
import ro.app.banking.model.TransactionType;
import ro.app.banking.repository.AccountRepository;
import ro.app.banking.repository.ClientRepository;
import ro.app.banking.repository.CurrencyTypeRepository;
import ro.app.banking.repository.TransactionRepository;
import ro.app.banking.repository.TransactionTypeRepository;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final ClientRepository clientRepository;
    private final TransactionRepository transactionRepository;
    private final CurrencyTypeRepository currencyRepository;
    private final TransactionTypeRepository transactionTypeRepository;

    public AccountService(AccountRepository accountRepository,
                          ClientRepository clientRepository,
                          TransactionRepository transactionRepository,
                          CurrencyTypeRepository currencyRepository,
                          TransactionTypeRepository transactionTypeRepository) {
        this.accountRepository = accountRepository;
        this.clientRepository = clientRepository;
        this.transactionRepository = transactionRepository;
        this.currencyRepository = currencyRepository;
        this.transactionTypeRepository = transactionTypeRepository;
    }

    private String generateIban(CurrencyType currency) {
        String currencyCode = currency.getCode().toUpperCase(); // eg: "RO", "EUR", "USD"

        String bankCode = "BANK";
        String accountNumber = String.format("%010d", (int)(Math.random() * 1_000_000_000));

        // IBAN starts with the currency code
        String iban = currencyCode + bankCode + accountNumber;

        // it’s unique in the database
        while (accountRepository.findByIban(iban).isPresent()) {
            accountNumber = String.format("%010d", (int)(Math.random() * 1_000_000_000));
            iban = currencyCode + bankCode + accountNumber;
        }

        return iban;
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

        CurrencyType currency = currencyRepository.findByCodeIgnoreCase(currencyCode)
                .orElseThrow(() -> new ResourceNotFoundException("Currency not found"));

        Account account = new Account();
        account.setClient(client);
        account.setCurrency(currency);
        account.setBalance(BigDecimal.ZERO);
        account.setStatus("ACTIV");

        // Generate IBAN based on the currency entity
        account.setIban(generateIban(currency));

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

        if ("INCHIS".equalsIgnoreCase(account.getStatus())) {
            throw new IllegalStateException("Account already closed");
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Account balance must be zero before closing");
        }

        account.setStatus("INCHIS");
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

    // 5️) Deposit money into an account
    @Transactional
    @CacheEvict(value= "balance", key= "#iban")
    public Transaction deposit(String iban, BigDecimal amount) {
        if(amount ==null || amount.compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("Amount must be positive");
        }

        Account account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        account.setBalance(account.getBalance().add(amount));

        TransactionType depositType = transactionTypeRepository.findByCodeIgnoreCase("DEP")
                .orElseThrow(() -> new ResourceNotFoundException("Transaction type 'DEP' not found"));

        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setAmount(amount);
        tx.setOriginalAmount(amount);
        tx.setOriginalCurrency(account.getCurrency());
        tx.setSign("+");
        tx.setTransactionType(depositType);
        tx.setTransactionDate(LocalDateTime.now());
        tx.setDetails("Deposit into account " + iban);

        transactionRepository.save(tx);
        accountRepository.save(account);
        return tx;
    }

    // 6️) Withdraw money from an account
    @Transactional
    @CacheEvict(value= "balance", key= "#iban")
    public Transaction withdraw(String iban, BigDecimal amount) {
        if(amount ==null || amount.compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("Amount must be positive");
        }

        Account account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        account.setBalance(account.getBalance().subtract(amount));

        TransactionType withdrawType = transactionTypeRepository.findByCodeIgnoreCase("RET")
                .orElseThrow(() -> new ResourceNotFoundException("Transaction type 'RET' not found"));

        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setAmount(amount);
        tx.setOriginalAmount(amount);
        tx.setOriginalCurrency(account.getCurrency());
        tx.setSign("-");
        tx.setTransactionType(withdrawType);
        tx.setTransactionDate(LocalDateTime.now());
        tx.setDetails("Withdrawal from account " + iban);

        transactionRepository.save(tx);
        accountRepository.save(account);
        return tx;
    }

    // 7️) Transfer between two accounts
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

        TransactionType transferType = transactionTypeRepository.findByCodeIgnoreCase("TRF")
                .orElseThrow(() -> new ResourceNotFoundException("Transaction type 'TRF' not found"));

        // update balances
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        // debit transaction
        Transaction debit = new Transaction();
        debit.setAccount(from);
        debit.setAmount(amount);
        debit.setOriginalAmount(amount);
        debit.setOriginalCurrency(from.getCurrency());
        debit.setSign("-");
        debit.setTransactionType(transferType);
        debit.setTransactionDate(LocalDateTime.now());
        debit.setDetails("Transfer to account " + toIban);

        // credit transaction
        Transaction credit = new Transaction();
        credit.setAccount(to);
        credit.setAmount(amount);
        credit.setOriginalAmount(amount);
        credit.setOriginalCurrency(to.getCurrency());
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
