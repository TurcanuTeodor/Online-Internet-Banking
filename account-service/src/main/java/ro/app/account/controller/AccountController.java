package ro.app.account.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import ro.app.account.dto.AccountDTO;
import ro.app.account.dto.mapper.AccountMapper;
import ro.app.account.dto.request.OpenAccountRequest;
import ro.app.account.dto.request.TransferRequest;
import ro.app.account.model.entity.Account;
import ro.app.account.service.AccountService;

@RestController
@RequestMapping("/api/accounts")
@Validated
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // 1) openAccount
    @PostMapping("/open")
    public ResponseEntity<AccountDTO> open(@Valid @RequestBody OpenAccountRequest req) {
        Account account = accountService.openAccount(req.getClientId(), req.getCurrencyCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountMapper.toDTO(account));
    }

    // 2) closeAccount
    @PostMapping("/{accountId}/close")
    public ResponseEntity<AccountDTO> close(@PathVariable Long accountId) {
        Account account = accountService.closeAccount(accountId);
        return ResponseEntity.ok(AccountMapper.toDTO(account));
    }

    // 3) getAccountsByClient
    @GetMapping("/by-client/{clientId}")
    public ResponseEntity<List<AccountDTO>> byClient(@PathVariable Long clientId) {
        List<Account> accounts = accountService.getAccountsByClient(clientId);
        return ResponseEntity.ok(accounts.stream().map(AccountMapper::toDTO).toList());
    }

    // 4) get balance by IBAN
    @GetMapping("/{iban}/balance")
    public ResponseEntity<BigDecimal> balance(
        @PathVariable
        @Pattern(regexp = "^[A-Z]{2}\\d{2}[A-Z0-9]{1,30}$", message = "Invalid IBAN format")
        String iban) {
        return ResponseEntity.ok(accountService.getBalanceByIban(iban));
    }

    // 5) transfer
    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(@Valid @RequestBody TransferRequest req) {
        accountService.transfer(req.getFromIban(), req.getToIban(), req.getAmount());
        return ResponseEntity.noContent().build();
    }

    // 6) Get all accounts from view (admin)
    @GetMapping("/view")
    public ResponseEntity<List<?>> viewAll() {
        return ResponseEntity.ok(accountService.getAllViewAccounts());
    }

    // 7) Freeze account (set status to SUSPENDED)
    @PostMapping("/{accountId}/freeze")
    public ResponseEntity<AccountDTO> freeze(@PathVariable Long accountId) {
        Account account = accountService.freezeAccount(accountId);
        return ResponseEntity.ok(AccountMapper.toDTO(account));
    }
}
