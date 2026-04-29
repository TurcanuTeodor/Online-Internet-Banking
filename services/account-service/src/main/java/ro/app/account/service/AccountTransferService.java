package ro.app.account.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import ro.app.account.audit.AuditService;
import ro.app.account.config.redis.CacheInvalidationPublisher;
import ro.app.account.exception.BusinessRuleViolationException;
import ro.app.account.exception.InsufficientFundsException;
import ro.app.account.exception.ResourceNotFoundException;
import ro.app.account.exception.StepUpRequiredException;
import ro.app.account.internal.InternalApiHeaders;
import ro.app.account.model.entity.Account;
import ro.app.account.model.enums.CurrencyType;
import ro.app.account.repository.AccountRepository;
import ro.app.account.security.JwtPrincipal;
import ro.app.account.security.OwnershipChecker;

/**
 * Internal transfers: balance updates + transaction records via transaction-service REST.
 */
@Service
public class AccountTransferService {

    private static final Logger log = LoggerFactory.getLogger(AccountTransferService.class);

    private final AccountRepository accountRepository;
    private final ExchangeRateService exchangeRateService;
    private final RestTemplate restTemplate;
    private final OwnershipChecker ownershipChecker;
    private final AuditService auditService;
    private final CacheInvalidationPublisher cacheInvalidationPublisher;
    private final RedissonClient redissonClient;
    private final String transactionServiceUrl;
    private final String fraudServiceUrl;
    private final String authServiceUrl;
    private final String internalApiSecret;
    private final BigDecimal largeTransferThreshold;

    public AccountTransferService(
            AccountRepository accountRepository,
            ExchangeRateService exchangeRateService,
            RestTemplate restTemplate,
            OwnershipChecker ownershipChecker,
            AuditService auditService,
            CacheInvalidationPublisher cacheInvalidationPublisher,
            RedissonClient redissonClient,
            @Value("${app.services.transaction.url}") String transactionServiceUrl,
            @Value("${app.services.auth.url:http://auth-service:8081}") String authServiceUrl,
            @Value("${app.services.fraud.url:}") String fraudServiceUrl,
            @Value("${app.internal.api-secret}") String internalApiSecret,
            @Value("${app.audit.large-transfer-threshold:1000}") BigDecimal largeTransferThreshold) {
        this.accountRepository = accountRepository;
        this.exchangeRateService = exchangeRateService;
        this.restTemplate = restTemplate;
        this.ownershipChecker = ownershipChecker;
        this.auditService = auditService;
        this.cacheInvalidationPublisher = cacheInvalidationPublisher;
        this.redissonClient = redissonClient;
        this.transactionServiceUrl = transactionServiceUrl.replaceAll("/$", "");
        this.authServiceUrl = authServiceUrl.replaceAll("/$", "");
        this.fraudServiceUrl = fraudServiceUrl != null ? fraudServiceUrl.replaceAll("/$", "") : "";
        this.internalApiSecret = internalApiSecret;
        this.largeTransferThreshold = largeTransferThreshold;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "balance", key = "'iban:' + #fromIban.trim().toUpperCase()"),
            @CacheEvict(value = "balance", key = "'iban:' + #toIban.trim().toUpperCase()"),
            @CacheEvict(value = "accountsByClient", allEntries = true),
            @CacheEvict(value = "accountDetails", allEntries = true)
    })
    public void transfer(String fromIban, String toIban, BigDecimal amount, String totpCode, JwtPrincipal principal) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (fromIban.equalsIgnoreCase(toIban)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        String normalizedFromIban = fromIban.trim().toUpperCase();
        String normalizedToIban = toIban.trim().toUpperCase();

        String firstLockKey = normalizedFromIban.compareTo(normalizedToIban) <= 0
                ? "lock:account:" + normalizedFromIban
                : "lock:account:" + normalizedToIban;
        String secondLockKey = normalizedFromIban.compareTo(normalizedToIban) <= 0
                ? "lock:account:" + normalizedToIban
                : "lock:account:" + normalizedFromIban;

        RLock firstLock = redissonClient.getLock(firstLockKey);
        RLock secondLock = redissonClient.getLock(secondLockKey);

        boolean firstLocked = false;
        boolean secondLocked = false;

        try {
            firstLocked = firstLock.tryLock(500, 10, TimeUnit.SECONDS);
            if (!firstLocked) {
                throw new BusinessRuleViolationException("Could not acquire transfer lock for first account");
            }

            secondLocked = secondLock.tryLock(500, 10, TimeUnit.SECONDS);
            if (!secondLocked) {
                throw new BusinessRuleViolationException("Could not acquire transfer lock for second account");
            }

            Account from = accountRepository.findByIban(normalizedFromIban)
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));
        ownershipChecker.checkOwnership(principal, from.getClientId());

            Account to = accountRepository.findByIban(normalizedToIban)
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));

            if (from.getBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException("Insufficient funds");
            }

            boolean isLargeTransfer = amount.compareTo(largeTransferThreshold) >= 0;

            if (isLargeTransfer) {
                Long actorClientId = principal != null ? principal.clientId() : null;
                String role = principal != null ? principal.role() : "UNKNOWN";
                auditService.log(
                        "LARGE_TRANSFER",
                        actorClientId,
                        role,
                        from.getClientId(),
                        "amount=" + amount + " " + from.getCurrency().getCode() + " fromIban=" + normalizedFromIban + " toIban=" + normalizedToIban);
            }
        
            boolean stepUpRequiredFromFraud = runFraudCheck(from, to, amount);

            if (isLargeTransfer || stepUpRequiredFromFraud) {
                if (totpCode == null || totpCode.isBlank()) {
                    throw new StepUpRequiredException("This transaction requires two-factor authentication.");
                }
                verifyStepUp(from.getClientId(), totpCode);
            }

            CurrencyType fromCurrency = from.getCurrency();
            CurrencyType toCurrency = to.getCurrency();
            BigDecimal convertedAmount = amount;
            if (fromCurrency != toCurrency) {
                BigDecimal rate = exchangeRateService.getRate(fromCurrency, toCurrency);
                convertedAmount = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            }

            from.setBalance(from.getBalance().subtract(amount));
            to.setBalance(to.getBalance().add(convertedAmount));

            accountRepository.save(from);
            accountRepository.save(to);
            publishInvalidations(from, "transfer");
            publishInvalidations(to, "transfer");

            try {
                String txUrl = transactionServiceUrl + "/api/transactions";
                LocalDateTime now = LocalDateTime.now();

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest request = attrs.getRequest();
                    String authHeader = request.getHeader("Authorization");
                    if (authHeader != null) {
                        headers.set("Authorization", authHeader);
                    }
                }

                Map<String, Object> debit = new HashMap<>();
                debit.put("accountId", from.getId());
                debit.put("destinationAccountId", to.getId());
                debit.put("transactionTypeCode", "TRANSFER_INTERNAL");
                debit.put("categoryCode", "OTHERS");
                debit.put("amount", amount);
                debit.put("originalAmount", amount);
                debit.put("originalCurrencyCode", fromCurrency.getCode());
                debit.put("sign", "-");
                debit.put("details", "Transfer to " + normalizedToIban);
                debit.put("transactionDate", now.toString());

                Map<String, Object> credit = new HashMap<>();
                credit.put("accountId", to.getId());
                credit.put("destinationAccountId", from.getId());
                credit.put("transactionTypeCode", "TRANSFER_INTERNAL");
                credit.put("categoryCode", "OTHERS");
                credit.put("amount", convertedAmount);
                credit.put("originalAmount", amount);
                credit.put("originalCurrencyCode", fromCurrency.getCode());
                credit.put("sign", "+");
                credit.put("details", "Transfer from " + normalizedFromIban);
                credit.put("transactionDate", now.toString());

                restTemplate.postForObject(txUrl, new HttpEntity<>(debit, headers), Map.class);
                restTemplate.postForObject(txUrl, new HttpEntity<>(credit, headers), Map.class);

            } catch (Exception e) {
                log.warn("Failed to create transaction records in transaction-service: {}", e.getMessage());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessRuleViolationException("Transfer interrupted while waiting for lock");
        } finally {
            if (secondLocked && secondLock.isHeldByCurrentThread()) {
                secondLock.unlock();
            }
            if (firstLocked && firstLock.isHeldByCurrentThread()) {
                firstLock.unlock();
            }
        }
    }

    private void publishInvalidations(Account account, String reason) {
        cacheInvalidationPublisher.publish("balance", "iban:" + account.getIban(), reason);
        cacheInvalidationPublisher.publish("accountsByClient", "client:" + account.getClientId(), reason);
        cacheInvalidationPublisher.publish("accountDetails", "id:" + account.getId(), reason);
        cacheInvalidationPublisher.publish("accountDetails", "iban:" + account.getIban(), reason);
    }

    private void verifyStepUp(Long clientId, String totpCode) {
        try {
            String url = authServiceUrl + "/api/internal/auth/step-up";
            Map<String, Object> body = new HashMap<>();
            body.put("clientId", clientId);
            body.put("totpCode", totpCode);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(InternalApiHeaders.SECRET, internalApiSecret);

            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Void.class);
        } catch (RestClientResponseException e) {
            int code = e.getStatusCode().value();
            if (code == 428) {
                throw new StepUpRequiredException("2FA must be enabled to perform this action.");
            }
            if (code == 401) {
                throw new BusinessRuleViolationException("Invalid 2FA code.");
            }
            log.warn("Step-up verification call failed: {} {}", code, e.getMessage());
            throw new BusinessRuleViolationException("Could not verify 2FA code.");
        } catch (Exception e) {
            log.warn("Step-up verification call failed: {}", e.getMessage());
            throw new BusinessRuleViolationException("Could not verify 2FA code.");
        }
    }

    @SuppressWarnings("unchecked")
    private boolean runFraudCheck(Account from, Account to, BigDecimal amount) {
        if (fraudServiceUrl == null || fraudServiceUrl.isBlank()) {
            log.debug("Fraud service URL not configured — skipping fraud check");
            return false;
        }

        try {
            String url = fraudServiceUrl + "/api/internal/fraud/evaluate";

            int accountAgeDays = (int) ChronoUnit.DAYS.between(from.getCreatedAt(), LocalDateTime.now());
            boolean selfTransfer = from.getClientId().equals(to.getClientId());

            Map<String, Object> body = new HashMap<>();
            body.put("accountId", from.getId());
            body.put("clientId", from.getClientId());
            body.put("amount", amount.doubleValue());
            body.put("currency", from.getCurrency().getCode());
            body.put("senderIban", from.getIban());
            body.put("receiverIban", to.getIban());
            body.put("transactionType", "TRANSFER_INTERNAL");
            body.put("selfTransfer", selfTransfer);
            body.put("accountAgeDays", accountAgeDays);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(InternalApiHeaders.SECRET, internalApiSecret);

            Map<String, Object> resp = restTemplate.postForObject(
                    url, new HttpEntity<>(body, headers), Map.class);

            if (resp != null && "BLOCK".equals(resp.get("status"))) {
                String explanation = (String) resp.getOrDefault("explanation", "Transaction blocked by fraud detection");
                log.warn("FRAUD BLOCK: client={} amount={} reason={}", from.getClientId(), amount, explanation);
                throw new BusinessRuleViolationException(explanation);
            }
            
            if (resp != null && "STEP_UP_REQUIRED".equals(resp.get("status"))) {
                log.info("FRAUD STEP-UP: client={} amount={}", from.getClientId(), amount);
                return true;
            }

            log.info("Fraud check passed: client={} status={}", from.getClientId(),
                    resp != null ? resp.get("status") : "unknown");

        } catch (BusinessRuleViolationException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Fraud service call failed — allowing transfer (fail-open): {}", e.getMessage());
        }
        return false;
    }
}
