package ro.app.transaction.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import ro.app.transaction.dto.TransactionDTO;
import ro.app.transaction.dto.mapper.TransactionMapper;
import ro.app.transaction.model.entity.Transaction;
import ro.app.transaction.model.view.ViewTransaction;
import ro.app.transaction.service.TransactionService;

@RestController
@RequestMapping("/api/transactions")
@Validated
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // 1) Get all transactions from view (read-only)
    @GetMapping("/view-all")
    public List<ViewTransaction> getAllFromView() {
        return transactionService.getAllView();
    }

    // 2) Get transactions by account ID
    @GetMapping("/by-account/{accountId}")
    public List<TransactionDTO> getByAccountId(@PathVariable Long accountId) {
        return transactionService.getTransactionsByAccountId(accountId)
                .stream()
                .map(TransactionMapper::toDTO)
                .toList();
    }

    // 3) Get transactions for multiple accounts (for a client's accounts)
    @GetMapping("/by-accounts")
    public List<TransactionDTO> getByAccountIds(@RequestParam List<Long> accountIds) {
        return transactionService.getTransactionsByAccountIds(accountIds)
                .stream()
                .map(TransactionMapper::toDTO)
                .toList();
    }

    // 4) Get transactions between two dates
    @GetMapping("/between")
    public List<TransactionDTO> getBetweenDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return transactionService.getTransactionsBetweenDates(from, to)
                .stream()
                .map(TransactionMapper::toDTO)
                .toList();
    }

    // 5) Get transactions by type code (DEPOSIT, WITHDRAWAL, TRANSFER_INTERNAL, TRANSFER_EXTERNAL)
    @GetMapping("/by-type/{code}")
    public List<TransactionDTO> getByType(@PathVariable String code) {
        return transactionService.getTransactionsByType(code)
                .stream()
                .map(TransactionMapper::toDTO)
                .toList();
    }

    // 6) Get daily totals (aggregated report)
    @GetMapping("/daily-totals")
    public Map<LocalDate, BigDecimal> getDailyTotals() {
        return transactionService.calculateDailyTotals();
    }

    // 7) Get flagged transactions
    @GetMapping("/flagged")
    public List<TransactionDTO> getFlagged() {
        return transactionService.getFlaggedTransactions()
                .stream()
                .map(TransactionMapper::toDTO)
                .toList();
    }

    // 8) Get a single transaction by ID
    @GetMapping("/{id}")
    public TransactionDTO getById(@PathVariable Long id) {
        return TransactionMapper.toDTO(transactionService.getById(id));
    }

    // 9) Create a new transaction (called by account-service or payment-service)
    @PostMapping
    public ResponseEntity<TransactionDTO> create(@Valid @RequestBody TransactionDTO dto) {
        Transaction entity = TransactionMapper.toEntity(dto);
        Transaction saved = transactionService.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionMapper.toDTO(saved));
    }
}
