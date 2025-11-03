package ro.app.backend_Java_SpringBoot.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.app.backend_Java_SpringBoot.exception.ResourceNotFoundException;
import ro.app.backend_Java_SpringBoot.model.*;
import ro.app.backend_Java_SpringBoot.repository.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionTypeRepository transactionTypeRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository,
                              TransactionTypeRepository transactionTypeRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.transactionTypeRepository = transactionTypeRepository;
    }

    //1) get transactions for account by iban
    public List<TransactionTable> getTransactionsByAccountIban(String iban) {
        return transactionRepository.findByAccountIban(iban);
    }

    //2) get transactions for client by clientId
    public List<TransactionTable> getTransactionsByClient(Long clientId) {
        return transactionRepository.findByClientId(clientId);
    }

    //3) get transactions between dates
    public List<TransactionTable> getTransactionsBetweenDates(LocalDate from, LocalDate to) {
        return transactionRepository.findBetweenDates(from, to);
    }

    //4) get transactions by code type
    public List<TransactionTable> getTransactionsByType(String code) {
        return transactionRepository.findByTransactionTypeCode(code);
    }

    //5) record of Transaction
    @Transactional
    public TransactionTable recordTransaction(TransactionTable tx) {
        if (tx.getAccount() == null) {
            throw new IllegalArgumentException("Transaction must be linked to an account");
        }

        AccountTable account = accountRepository.findById(tx.getAccount().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (tx.getSign() == null || (!tx.getSign().equals("+") && !tx.getSign().equals("-"))) {
            throw new IllegalArgumentException("Invalid transaction sign: must be '+' or '-'");
        }

        // ajustare sold
        if (tx.getSign().equals("+")) {
            account.setBalance(account.getBalance().add(tx.getAmount()));
        } else {
            if (account.getBalance().compareTo(tx.getAmount()) < 0) {
                throw new IllegalArgumentException("Insufficient funds");
            }
            account.setBalance(account.getBalance().subtract(tx.getAmount()));
        }

        //data setată
        if (tx.getTransactionDate() == null) {
            tx.setTransactionDate(LocalDateTime.now());
        }

        //valuta și tipul corect
        if (tx.getOriginalCurrency() == null) {
            tx.setOriginalCurrency(account.getCurrency());
        }

        if (tx.getTransactionType() == null) {
            TransactionType defaultType = transactionTypeRepository.findByCodeIgnoreCase("MAN")
                    .orElseThrow(() -> new ResourceNotFoundException("Default transaction type 'MAN' not found"));
            tx.setTransactionType(defaultType);
        }

        accountRepository.save(account);
        return transactionRepository.save(tx);
    }

    // 6) calculateDailyTotals
    public Map<LocalDate, BigDecimal> calculateDailyTotals() {
        Map<LocalDate, BigDecimal> result = new LinkedHashMap<>();
        List<Object[]> data = transactionRepository.calculateDailyTotals();

        for (Object[] row : data) {
            LocalDate date;
            if (row[0] instanceof Timestamp ts) {
                date = ts.toLocalDateTime().toLocalDate();
            } else if (row[0] instanceof LocalDateTime ldt) {
                date = ldt.toLocalDate();
            } else if (row[0] instanceof LocalDate ld) {
                date = ld;
            } else {
                throw new IllegalStateException("Unsupported date type: " + row[0].getClass());
            }

            BigDecimal total = (BigDecimal) row[1];
            result.put(date, total);
        }

        return result;
    }
}
