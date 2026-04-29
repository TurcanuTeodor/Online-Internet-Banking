package ro.app.account.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import ro.app.account.config.redis.CacheInvalidationPublisher;
import ro.app.account.exception.BusinessRuleViolationException;
import ro.app.account.exception.ResourceNotFoundException;
import ro.app.account.internal.InternalApiHeaders;
import ro.app.account.internal.StripeTopUpApplyRequest;
import ro.app.account.model.entity.Account;
import ro.app.account.model.enums.AccountStatus;
import ro.app.account.repository.AccountRepository;

/**
 * Stripe top-up: credit balance + internal DEPOSIT call to transaction-service.
 */
@Service
public class AccountTopUpService {

    private final AccountRepository accountRepository;
    private final RestTemplate restTemplate;
    private final String transactionServiceUrl;
    private final String internalApiSecret;
    private final CacheInvalidationPublisher cacheInvalidationPublisher;

    public AccountTopUpService(
            AccountRepository accountRepository,
            RestTemplate restTemplate,
            CacheInvalidationPublisher cacheInvalidationPublisher,
            @Value("${app.services.transaction.url}") String transactionServiceUrl,
            @Value("${app.internal.api-secret}") String internalApiSecret) {
        this.accountRepository = accountRepository;
        this.restTemplate = restTemplate;
        this.cacheInvalidationPublisher = cacheInvalidationPublisher;
        this.transactionServiceUrl = transactionServiceUrl.replaceAll("/$", "");
        this.internalApiSecret = internalApiSecret;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "balance", key = "'iban:' + #result.iban"),
            @CacheEvict(value = "accountsByClient", key = "'client:' + #result.clientId"),
            @CacheEvict(value = "accountDetails", allEntries = true)
    })
    public Account applyStripeTopUpCredit(StripeTopUpApplyRequest req) {
        Account account = accountRepository.findById(req.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!AccountStatus.ACTIVE.equals(account.getStatus())) {
            throw new BusinessRuleViolationException("Account must be ACTIVE for top-up");
        }
        if (!account.getCurrency().getCode().equalsIgnoreCase(req.getCurrencyCode())) {
            throw new BusinessRuleViolationException("Currency mismatch between payment and account");
        }

        BigDecimal amount = req.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleViolationException("Top-up amount must be positive");
        }

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        publishInvalidations(account, "topup");

        postInternalStripeDeposit(account, amount, req.getStripePaymentIntentId());
        return account;
    }

    private void publishInvalidations(Account account, String reason) {
        cacheInvalidationPublisher.publish("balance", "iban:" + account.getIban(), reason);
        cacheInvalidationPublisher.publish("accountsByClient", "client:" + account.getClientId(), reason);
        cacheInvalidationPublisher.publish("accountDetails", "id:" + account.getId(), reason);
        cacheInvalidationPublisher.publish("accountDetails", "iban:" + account.getIban(), reason);
    }

    private void postInternalStripeDeposit(Account account, BigDecimal amount, String stripePaymentIntentId) {
        String url = transactionServiceUrl + "/api/internal/transactions/deposit";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(InternalApiHeaders.SECRET, internalApiSecret);

        Map<String, Object> body = new HashMap<>();
        body.put("accountId", account.getId());
        body.put("destinationAccountId", null);
        body.put("transactionTypeCode", "DEPOSIT");
        body.put("categoryCode", "INCOME");
        body.put("amount", amount);
        body.put("originalAmount", amount);
        body.put("originalCurrencyCode", account.getCurrency().getCode());
        body.put("sign", "+");
        body.put("merchant", "Stripe");
        body.put("details", "Card top-up (" + stripePaymentIntentId + ")");
        body.put("transactionDate", LocalDateTime.now().toString());
        body.put("riskScore", BigDecimal.ZERO);
        body.put("flagged", false);

        restTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);
    }
}
