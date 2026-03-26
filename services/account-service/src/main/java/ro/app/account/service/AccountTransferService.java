package ro.app.account.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import ro.app.account.audit.AuditService;
import ro.app.account.exception.InsufficientFundsException;
import ro.app.account.exception.ResourceNotFoundException;
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
    private final String transactionServiceUrl;
    private final BigDecimal largeTransferThreshold;

    public AccountTransferService(
            AccountRepository accountRepository,
            ExchangeRateService exchangeRateService,
            RestTemplate restTemplate,
            OwnershipChecker ownershipChecker,
            AuditService auditService,
            @Value("${app.services.transaction.url}") String transactionServiceUrl,
            @Value("${app.audit.large-transfer-threshold:10000}") BigDecimal largeTransferThreshold) {
        this.accountRepository = accountRepository;
        this.exchangeRateService = exchangeRateService;
        this.restTemplate = restTemplate;
        this.ownershipChecker = ownershipChecker;
        this.auditService = auditService;
        this.transactionServiceUrl = transactionServiceUrl.replaceAll("/$", "");
        this.largeTransferThreshold = largeTransferThreshold;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "balance", key = "#fromIban"),
            @CacheEvict(value = "balance", key = "#toIban"),
            @CacheEvict(value = "accountsByClient", allEntries = true)
    })
    public void transfer(String fromIban, String toIban, BigDecimal amount, JwtPrincipal principal) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (fromIban.equalsIgnoreCase(toIban)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        Account from = accountRepository.findByIban(fromIban)
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));
        ownershipChecker.checkOwnership(principal, from.getClientId());

        Account to = accountRepository.findByIban(toIban)
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));

        if (from.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        if (amount.compareTo(largeTransferThreshold) >= 0) {
            Long actorClientId = principal != null ? principal.clientId() : null;
            String role = principal != null ? principal.role() : "UNKNOWN";
            auditService.log(
                    "LARGE_TRANSFER",
                    actorClientId,
                    role,
                    from.getClientId(),
                    "amount=" + amount + " " + from.getCurrency().getCode() + " fromIban=" + fromIban + " toIban=" + toIban);
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
            debit.put("details", "Transfer to " + toIban);
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
            credit.put("details", "Transfer from " + fromIban);
            credit.put("transactionDate", now.toString());

            restTemplate.postForObject(txUrl, new HttpEntity<>(debit, headers), Map.class);
            restTemplate.postForObject(txUrl, new HttpEntity<>(credit, headers), Map.class);

        } catch (Exception e) {
            log.warn("Failed to create transaction records in transaction-service: {}", e.getMessage());
        }
    }
}
