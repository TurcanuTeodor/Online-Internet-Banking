package ro.app.banking.controller;

import jakarta.validation.Valid;
import ro.app.banking.dto.*;
import ro.app.banking.dto.mapper.*;
import ro.app.banking.dto.request.*;
import ro.app.banking.model.Account;
import ro.app.banking.model.Transaction;
import ro.app.banking.service.AccountService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

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
    public ResponseEntity<BigDecimal> balance(@PathVariable String iban) {
        return ResponseEntity.ok(accountService.getBalanceByIban(iban));
    }

    // 5) deposit
    @PostMapping("/{iban}/deposit")
    public ResponseEntity<TransactionDTO> deposit(@PathVariable String iban, @Valid @RequestBody AmountRequest req) {
        Transaction transaction = accountService.deposit(iban, req.getAmount());
        return ResponseEntity.ok(TransactionMapper.toDTO(transaction));
    }

    // 6) withdraw
    @PostMapping("/{iban}/withdraw")
    public ResponseEntity<TransactionDTO> withdraw(@PathVariable String iban, @Valid @RequestBody AmountRequest req) {
        Transaction transaction = accountService.withdraw(iban, req.getAmount());
        return ResponseEntity.ok(TransactionMapper.toDTO(transaction));
    }

    // 7) transfer
     @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(@Valid @RequestBody TransferRequest req) {
        accountService.transfer(req.getFromIban(), req.getToIban(), req.getAmount());
        return ResponseEntity.noContent().build();
    }

}
