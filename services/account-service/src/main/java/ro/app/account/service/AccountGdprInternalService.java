package ro.app.account.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.app.account.model.entity.Account;
import ro.app.account.model.enums.AccountStatus;
import ro.app.account.repository.AccountRepository;

/**
 * Internal GDPR: list account IDs and bulk-close accounts for a client.
 */
@Service
public class AccountGdprInternalService {

    private final AccountRepository accountRepository;

    public AccountGdprInternalService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public List<Long> listAccountIdsByClient(Long clientId) {
        return accountRepository.findByClientId(clientId).stream().map(Account::getId).toList();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "accountsByClient", key = "#clientId"),
            @CacheEvict(value = "balance", allEntries = true)
    })
    public int closeAllAccountsForClient(Long clientId) {
        return accountRepository.gdprCloseAllByClientId(clientId, AccountStatus.CLOSED);
    }
}
