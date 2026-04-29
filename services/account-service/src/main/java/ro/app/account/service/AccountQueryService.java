package ro.app.account.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import ro.app.account.dto.AccountDTO;
import ro.app.account.dto.mapper.AccountMapper;
import ro.app.account.exception.ResourceNotFoundException;
import ro.app.account.model.entity.Account;
import ro.app.account.repository.AccountRepository;
import ro.app.account.repository.ViewAccountRepository;
import ro.app.account.security.JwtPrincipal;
import ro.app.account.security.OwnershipChecker;

/**
 * Read paths and ownership-guarded DTO/balance lookups — no cross-service HTTP.
 */
@Service
public class AccountQueryService {

    private final AccountRepository accountRepository;
    private final ViewAccountRepository viewAccountRepository;
    private final OwnershipChecker ownershipChecker;

    public AccountQueryService(
            AccountRepository accountRepository,
            ViewAccountRepository viewAccountRepository,
            OwnershipChecker ownershipChecker) {
        this.accountRepository = accountRepository;
        this.viewAccountRepository = viewAccountRepository;
        this.ownershipChecker = ownershipChecker;
    }

    @Cacheable(value = "accountsByClient", key = "'client:' + #clientId")
    public List<Account> getAccountsByClient(Long clientId) {
        return accountRepository.findByClientId(clientId);
    }

    /**
     * Single account by id for authenticated user (or admin). Used by payment top-up flow.
     */
    @Cacheable(value = "accountDetails", key = "'id:' + #accountId")
    public AccountDTO getAccountDtoForPrincipal(Long accountId, JwtPrincipal principal) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        ownershipChecker.checkOwnership(principal, account.getClientId());
        return AccountMapper.toDTO(account);
    }

    /**
     * Account by IBAN for authenticated user (or admin). Used by transaction-service for statement-by-IBAN.
     */
    @Cacheable(value = "accountDetails", key = "'iban:' + #iban.trim().toUpperCase()")
    public AccountDTO getAccountDtoByIban(String iban, JwtPrincipal principal) {
        String normalized = iban != null ? iban.trim().toUpperCase() : "";
        Account account = accountRepository.findByIban(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        ownershipChecker.checkOwnership(principal, account.getClientId());
        return AccountMapper.toDTO(account);
    }

    @Cacheable(value = "balance", key = "'iban:' + #iban.trim().toUpperCase()")
    public BigDecimal getBalanceByIban(String iban, JwtPrincipal principal) {
        Account account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        ownershipChecker.checkOwnership(principal, account.getClientId());
        return account.getBalance();
    }

    public List<?> getAllViewAccounts() {
        return viewAccountRepository.findAll();
    }
}
