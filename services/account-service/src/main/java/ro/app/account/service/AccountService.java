package ro.app.account.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
import jakarta.validation.constraints.NotNull;
import ro.app.account.exception.BusinessRuleViolationException;
import ro.app.account.exception.InsufficientFundsException;
import ro.app.account.exception.ResourceNotFoundException;
import ro.app.account.model.entity.Account;
import ro.app.account.model.enums.AccountStatus;
import ro.app.account.model.enums.CurrencyType;
import ro.app.account.repository.AccountRepository;
import ro.app.account.repository.ViewAccountRepository;

import ro.app.account.security.JwtPrincipal;
import ro.app.account.security.OwnershipChecker;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final IbanService ibanService;
    private final ExchangeRateService exchangeRateService;
    private final ViewAccountRepository viewAccountRepository;
    private final RestTemplate restTemplate;
    private final OwnershipChecker ownershipChecker;

    public AccountService(AccountRepository accountRepository,
                          IbanService ibanService,
                          ExchangeRateService exchangeRateService,
                          ViewAccountRepository viewAccountRepository,
                          RestTemplate restTemplate,
                          OwnershipChecker ownershipChecker) {
        this.accountRepository = accountRepository;
        this.ibanService = ibanService;
        this.exchangeRateService = exchangeRateService;
        this.viewAccountRepository = viewAccountRepository;
        this.restTemplate = restTemplate;
        this.ownershipChecker = ownershipChecker;
    }

    // Distributed: no ClientRepository — clientId accepted as-is
    // Distributed: no TransactionRepository — transaction-service owns transactions

    // 1) Open a new account
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

        // Generate unique IBAN
        String iban = ibanService.generateIban(accountRepository::existsByIban);
        account.setIban(iban);

        return accountRepository.save(account);
    }

    // 2) Close an existing account
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

    // 3) Get all accounts for a specific client
    @Cacheable(value = "accountsByClient", key = "#clientId")
    public List<Account> getAccountsByClient(Long clientId) {
        return accountRepository.findByClientId(clientId);
    }

    // 4) Get balance by IBAN
    @Cacheable(value = "balance", key = "#iban")
    public BigDecimal getBalanceByIban(String iban, JwtPrincipal principal) {
        Account account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        ownershipChecker.checkOwnership(principal, account.getClientId());
        return account.getBalance();    }

    // 5) Transfer between two accounts (balance update only)
    // Distributed: transaction records are created by transaction-service, not here
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

        // Currency conversion if needed
        CurrencyType fromCurrency = from.getCurrency();
        CurrencyType toCurrency = to.getCurrency();
        BigDecimal convertedAmount = amount;
        if (fromCurrency != toCurrency) {
            BigDecimal rate = exchangeRateService.getRate(fromCurrency, toCurrency);
            convertedAmount = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        }

        // Update balances
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(convertedAmount));

        accountRepository.save(from);
        accountRepository.save(to);

        // Create transaction records in transaction-service
        try {
            String txUrl = "http://localhost:8084/api/transactions";
            LocalDateTime now = LocalDateTime.now();

            // Forward the JWT token from the incoming request
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

            // Debit record (source account)
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

            // Credit record (destination account)
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

    // 6) Get all accounts from VIEW_ACCOUNT (admin)
    public List<?> getAllViewAccounts() {
        return viewAccountRepository.findAll();
    }

    // 7) Freeze an account (set status to SUSPENDED)
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
