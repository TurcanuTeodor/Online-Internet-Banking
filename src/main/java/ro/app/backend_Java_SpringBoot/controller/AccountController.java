package ro.app.backend_Java_SpringBoot.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ro.app.backend_Java_SpringBoot.dto.AccountDTO;
import ro.app.backend_Java_SpringBoot.dto.mapper.AccountMapper;
import ro.app.backend_Java_SpringBoot.model.AccountTable;
import ro.app.backend_Java_SpringBoot.service.AccountService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    public AccountController(AccountService accountService) { this.accountService = accountService; }

    // 1) openAccount
    @PostMapping("/open")
    public ResponseEntity<AccountDTO> open(@Valid @RequestBody OpenAccountRequest req) {
        AccountTable acc = accountService.openAccount(req.clientId(), req.currencyCode());
        return ResponseEntity.ok(AccountMapper.toDTO(acc));
    }

    // 2) closeAccount
    @PostMapping("/{accountId}/close")
    public ResponseEntity<Void> close(@PathVariable Long accountId) {
        accountService.closeAccount(accountId);
        return ResponseEntity.noContent().build();
    }

    // 3) getAccountsByClient
    @GetMapping("/by-client/{clientId}")
    public List<AccountTable> byClient(@PathVariable Long clientId) {
        return accountService.getAccountsByClient(clientId);
    }

    // 4) get balance by IBAN
    @GetMapping("/{iban}/balance")
    public BigDecimal balance(@PathVariable String iban) {
        return accountService.getBalanceByIban(iban);
    }

    // 5) deposit
    @PostMapping("/{iban}/deposit")
    public ResponseEntity<?> deposit(@PathVariable String iban, @RequestBody AmountRequest req) {
        return ResponseEntity.ok(accountService.deposit(iban, req.amount()));
    }

    // 6) withdraw
    @PostMapping("/{iban}/withdraw")
    public ResponseEntity<?> withdraw(@PathVariable String iban, @RequestBody AmountRequest req) {
        return ResponseEntity.ok(accountService.withdraw(iban, req.amount()));
    }

    // 7) transfer
    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(@RequestBody TransferRequest req) {
        accountService.transfer(req.fromIban(), req.toIban(), req.amount());
        return ResponseEntity.noContent().build();
    }

    // --- request bodies ---
    public record OpenAccountRequest(@NotNull Long clientId,
                                     @NotBlank String currencyCode) {}

    public record AmountRequest(@NotNull @Digits(integer=15, fraction=2) BigDecimal amount) {}

    public record TransferRequest(@NotBlank String fromIban,
                                  @NotBlank String toIban,
                                  @NotNull @Digits(integer=15, fraction=2) BigDecimal amount) {}
}
