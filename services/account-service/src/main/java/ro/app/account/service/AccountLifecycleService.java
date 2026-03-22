package ro.app.account.service;

import java.math.BigDecimal;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.constraints.NotNull;
import ro.app.account.exception.BusinessRuleViolationException;
import ro.app.account.exception.ResourceNotFoundException;
import ro.app.account.model.entity.Account;
import ro.app.account.model.enums.AccountStatus;
import ro.app.account.model.enums.CurrencyType;
import ro.app.account.repository.AccountRepository;

/**
 * Account provisioning and status changes (open / close / freeze).
 */
@Service
public class AccountLifecycleService {

    private final AccountRepository accountRepository;
    private final IbanService ibanService;

    public AccountLifecycleService(AccountRepository accountRepository, IbanService ibanService) {
        this.accountRepository = accountRepository;
        this.ibanService = ibanService;
    }

    @Transactional
    @CacheEvict(value = "accountsByClient", key = "#clientId")
    public Account openAccount(@NotNull Long clientId, String currencyCode) {
        if (clientId == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }

        CurrencyType currency = CurrencyType.fromCode(currencyCode);

        Account account = new Account();
        account.setClientId(clientId);
        account.setCurrency(currency);
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);

        String iban = ibanService.generateIban(accountRepository::existsByIban);
        account.setIban(iban);

        return accountRepository.save(account);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "accountsByClient", key = "#result.clientId"),
            @CacheEvict(value = "balance", key = "#result.iban")
    })
    public Account closeAccount(@NotNull Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (AccountStatus.CLOSED.equals(account.getStatus())) {
            throw new BusinessRuleViolationException("Account already closed");
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessRuleViolationException("Account balance must be zero before closing");
        }

        account.setStatus(AccountStatus.CLOSED);
        return accountRepository.save(account);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "accountsByClient", key = "#result.clientId"),
            @CacheEvict(value = "balance", key = "#result.iban")
    })
    public Account freezeAccount(@NotNull Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (AccountStatus.SUSPENDED.equals(account.getStatus())) {
            throw new BusinessRuleViolationException("Account already frozen");
        }

        if (AccountStatus.CLOSED.equals(account.getStatus())) {
            throw new BusinessRuleViolationException("Cannot freeze a closed account");
        }

        account.setStatus(AccountStatus.SUSPENDED);
        return accountRepository.save(account);
    }
}
