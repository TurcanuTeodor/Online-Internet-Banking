package ro.app.backend_Java_SpringBoot.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ro.app.backend_Java_SpringBoot.dto.AccountDTO;
import ro.app.backend_Java_SpringBoot.dto.mapper.AccountMapper;
import ro.app.backend_Java_SpringBoot.model.AccountTable;
import ro.app.backend_Java_SpringBoot.model.TransactionTable;
import ro.app.backend_Java_SpringBoot.service.AccountService;
import ro.app.backend_Java_SpringBoot.dto.request.OpenAccountRequest;
import ro.app.backend_Java_SpringBoot.dto.request.AmountRequest;
import ro.app.backend_Java_SpringBoot.dto.request.TransferRequest;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    public AccountController(AccountService accountService) { 
        this.accountService = accountService; 
    }

    // 1) openAccount
    @PostMapping("/open")
    public ResponseEntity<AccountDTO> open(@Valid @RequestBody OpenAccountRequest req) {
        AccountTable account = accountService.openAccount(req.getClientId(), req.getCurrencyCode());
        return ResponseEntity.ok(AccountMapper.toDTO(account));
    }

    // 2) closeAccount
    @PostMapping("/{accountId}/close")
    public ResponseEntity<AccountDTO> close(@PathVariable Long accountId) {
        AccountTable account = accountService.closeAccount(accountId);
        return ResponseEntity.ok(AccountMapper.toDTO(account));
    }

    // 3) getAccountsByClient
    @GetMapping("/by-client/{clientId}")
    public ResponseEntity<List<AccountDTO>> byClient(@PathVariable Long clientId) {
        List<AccountTable> accounts = accountService.getAccountsByClient(clientId);
        return ResponseEntity.ok(accounts.stream().map(AccountMapper::toDTO).toList());
    }

    // 4) get balance by IBAN
    @GetMapping("/{iban}/balance")
    public ResponseEntity<BigDecimal> balance(@PathVariable String iban) {
        return ResponseEntity.ok(accountService.getBalanceByIban(iban));
    }

    // 5) deposit
    @PostMapping("/{iban}/deposit")
    public ResponseEntity<TransactionTable> deposit(@PathVariable String iban, @Valid @RequestBody AmountRequest req) {
        return ResponseEntity.ok(accountService.deposit(iban, req.getAmount()));
    }

    // 6) withdraw
    @PostMapping("/{iban}/withdraw")
    public ResponseEntity<TransactionTable> withdraw(@PathVariable String iban, @Valid @RequestBody AmountRequest req) {
        return ResponseEntity.ok(accountService.withdraw(iban, req.getAmount()));
    }

    // 7) transfer
     @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(@Valid @RequestBody TransferRequest req) {
        accountService.transfer(req.getFromIban(), req.getToIban(), req.getAmount());
        return ResponseEntity.noContent().build();
    }

}
