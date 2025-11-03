package ro.app.backend_Java_SpringBoot.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.app.backend_Java_SpringBoot.exception.ResourceNotFoundException;
import ro.app.backend_Java_SpringBoot.model.*;
import ro.app.backend_Java_SpringBoot.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    private String generateIban(String currencyCode) {
        String iban;
        do {
            iban = "RO" + currencyCode.toUpperCase() + String.format("%010d", (int)(Math.random() * 1_000_000_000));
        } while (accountRepository.findByIban(iban).isPresent());
        return iban;
    }

    // --1 openAccount
    @Transactional
    public AccountTable openAccount(Long clientId, String currencyCode) {
        ClientTable client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        CurrencyType currency = currencyRepository.findByCodeIgnoreCase(currencyCode)
                .orElseThrow(() -> new ResourceNotFoundException("Currency not found"));

        AccountTable account = new AccountTable();
        account.setClient(client);
        account.setCurrency(currency);
        account.setBalance(BigDecimal.ZERO);
        account.setStatus("ACTIV");
        account.setIban(generateIban(currencyCode));

        return accountRepository.save(account);
    }

    // --2 closeAccount
    @Transactional
    public AccountTable closeAccount(Long id) {
        AccountTable account = accountRepository.findById(id)
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

    // --3 getAccountsByClient
    public List<AccountTable> getAccountsByClient(Long clientId) {
        return accountRepository.findByClientId(clientId);
    }

    // --4 getBalanceByIban
    public BigDecimal getBalanceByIban(String iban) {
        return accountRepository.findByIban(iban)
                .map(AccountTable::getBalance)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }

    // --5 deposit
    @Transactional
    public TransactionTable deposit(String iban, BigDecimal amount) {
        AccountTable account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        account.setBalance(account.getBalance().add(amount));

        TransactionType depositType = transactionTypeRepository.findByCodeIgnoreCase("DEP")
                .orElseThrow(() -> new ResourceNotFoundException("Transaction type 'DEP' not found"));

        TransactionTable tx = new TransactionTable();
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

    // --6 withdraw
    @Transactional
    public TransactionTable withdraw(String iban, BigDecimal amount) {
        AccountTable account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        account.setBalance(account.getBalance().subtract(amount));

        TransactionType withdrawType = transactionTypeRepository.findByCodeIgnoreCase("RET")
                .orElseThrow(() -> new ResourceNotFoundException("Transaction type 'RET' not found"));

        TransactionTable tx = new TransactionTable();
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

    // --7 transfer
    @Transactional
    public void transfer(String fromIban, String toIban, BigDecimal amount) {
        if (fromIban.equalsIgnoreCase(toIban)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        AccountTable from = accountRepository.findByIban(fromIban)
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));
        AccountTable to = accountRepository.findByIban(toIban)
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));

        if (from.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        TransactionType transferType = transactionTypeRepository.findByCodeIgnoreCase("TRF")
                .orElseThrow(() -> new ResourceNotFoundException("Transaction type 'TRF' not found"));

        // actualizare solduri
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        // tranzacție de debit
        TransactionTable t1 = new TransactionTable();
        t1.setAccount(from);
        t1.setAmount(amount);
        t1.setOriginalAmount(amount);
        t1.setOriginalCurrency(from.getCurrency());
        t1.setSign("-");
        t1.setTransactionType(transferType);
        t1.setTransactionDate(LocalDateTime.now());
        t1.setDetails("Transfer to account " + toIban);

        // tranzacție de credit
        TransactionTable t2 = new TransactionTable();
        t2.setAccount(to);
        t2.setAmount(amount);
        t2.setOriginalAmount(amount);
        t2.setOriginalCurrency(to.getCurrency());
        t2.setSign("+");
        t2.setTransactionType(transferType);
        t2.setTransactionDate(LocalDateTime.now());
        t2.setDetails("Transfer from account " + fromIban);

        transactionRepository.save(t1);
        transactionRepository.save(t2);
        accountRepository.save(from);
        accountRepository.save(to);
    }
}
