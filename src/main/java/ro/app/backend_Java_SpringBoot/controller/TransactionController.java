package ro.app.backend_Java_SpringBoot.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import ro.app.backend_Java_SpringBoot.DTO.TransactionDTO;
import ro.app.backend_Java_SpringBoot.DTO.mapper.TransactionMapper;

import org.springframework.web.bind.annotation.*;
import ro.app.backend_Java_SpringBoot.model.*;
import ro.app.backend_Java_SpringBoot.service.TransactionService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @PersistenceContext
    private EntityManager em;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // 1) toate tranzacțiile după IBAN
    @GetMapping("/by-iban/{iban}")
    public List<TransactionTable> byIban(@PathVariable String iban) {
        return transactionService.getTransactionsByAccountIban(iban);
    }

    // 2) toate tranzacțiile unui client
    @GetMapping("/by-client/{clientId}")
    public List<TransactionTable> byClient(@PathVariable Long clientId) {
        return transactionService.getTransactionsByClient(clientId);
    }

    // 3) între două date
    @GetMapping("/between")
    public List<TransactionTable> between(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return transactionService.getTransactionsBetweenDates(from, to);
    }

    // 4) după tip (cod)
    @GetMapping("/by-type/{code}")
    public List<TransactionTable> byType(@PathVariable @NotBlank String code) {
        return transactionService.getTransactionsByType(code);
    }

    // 5) record manual de tranzacție
    @PostMapping
    public ResponseEntity<TransactionDTO> record(@Valid @RequestBody TransactionDTO dto) {
        AccountTable account = em.getReference(AccountTable.class, dto.getAccountId());
        TransactionType type = em.getReference(TransactionType.class, dto.getTransactionTypeId());
        CurrencyType origCur = em.getReference(CurrencyType.class, dto.getOriginalCurrencyId());

        TransactionTable entity = TransactionMapper.toEntity(dto, account, type, origCur);
        TransactionTable saved = transactionService.recordTransaction(entity);
        return ResponseEntity.ok(TransactionMapper.toDTO(saved));
    }

    // 6) totaluri zilnice 
    @GetMapping("/daily-totals")
    public Map<LocalDate, java.math.BigDecimal> dailyTotals() {
        return transactionService.calculateDailyTotals();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDTO> getById(@PathVariable Long id) {
        var entity = transactionService.findById(id);
        var dto = TransactionMapper.toDTO(entity);
        return ResponseEntity.ok(dto);
    }
}
