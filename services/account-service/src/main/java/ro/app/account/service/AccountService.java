package ro.app.account.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import ro.app.account.dto.AccountDTO;
import ro.app.account.internal.StripeTopUpApplyRequest;
import ro.app.account.model.entity.Account;
import ro.app.account.security.JwtPrincipal;

/**
 * Facade — delegates to {@link AccountQueryService}, {@link AccountLifecycleService},
 * {@link AccountTransferService}, {@link AccountTopUpService}. Public API unchanged for controllers.
 */
@Service
public class AccountService {

    private final AccountQueryService accountQueryService;
    private final AccountLifecycleService accountLifecycleService;
    private final AccountTransferService accountTransferService;
    private final AccountTopUpService accountTopUpService;

    public AccountService(
            AccountQueryService accountQueryService,
            AccountLifecycleService accountLifecycleService,
            AccountTransferService accountTransferService,
            AccountTopUpService accountTopUpService) {
        this.accountQueryService = accountQueryService;
        this.accountLifecycleService = accountLifecycleService;
        this.accountTransferService = accountTransferService;
        this.accountTopUpService = accountTopUpService;
    }

    public Account openAccount(Long clientId, String currencyCode) {
        return accountLifecycleService.openAccount(clientId, currencyCode);
    }

    public Account closeAccount(Long id) {
        return accountLifecycleService.closeAccount(id);
    }

    public List<Account> getAccountsByClient(Long clientId) {
        return accountQueryService.getAccountsByClient(clientId);
    }

    public AccountDTO getAccountDtoForPrincipal(Long accountId, JwtPrincipal principal) {
        return accountQueryService.getAccountDtoForPrincipal(accountId, principal);
    }

    public AccountDTO getAccountDtoByIban(String iban, JwtPrincipal principal) {
        return accountQueryService.getAccountDtoByIban(iban, principal);
    }

    public Account applyStripeTopUpCredit(StripeTopUpApplyRequest req) {
        return accountTopUpService.applyStripeTopUpCredit(req);
    }

    public BigDecimal getBalanceByIban(String iban, JwtPrincipal principal) {
        return accountQueryService.getBalanceByIban(iban, principal);
    }

    public void transfer(String fromIban, String toIban, BigDecimal amount, String totpCode, JwtPrincipal principal) {
        accountTransferService.transfer(fromIban, toIban, amount, totpCode, principal);
    }

    public List<?> getAllViewAccounts() {
        return accountQueryService.getAllViewAccounts();
    }

    public Account freezeAccount(Long id) {
        return accountLifecycleService.freezeAccount(id);
    }

    public Account unfreezeAccount(Long id) {
        return accountLifecycleService.unfreezeAccount(id);
    }
}
