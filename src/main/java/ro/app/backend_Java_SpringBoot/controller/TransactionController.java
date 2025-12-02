package ro.app.backend_Java_SpringBoot.controller;

import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ro.app.backend_Java_SpringBoot.dto.TransactionDTO;
import ro.app.backend_Java_SpringBoot.dto.mapper.TransactionMapper;
import ro.app.backend_Java_SpringBoot.model.ViewTransaction;
import ro.app.backend_Java_SpringBoot.service.TransactionService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // 1️) Get all transactions from view (read-only)
    @GetMapping("/view-all")
    public List<ViewTransaction> getAllFromView() {
        return transactionService.getAllView();
    }

    // 2️) Get transactions by IBAN (from real table)
    @GetMapping("/by-iban/{iban}")
    public List<TransactionDTO> getByIban(@PathVariable @NotBlank String iban) {
        return transactionService.getTransactionsByAccountIban(iban)
                .stream()
                .map(TransactionMapper::toDTO)
                .toList();
    }

    // 3️) Get transactions by client ID
    @GetMapping("/by-client/{clientId}")
    public List<TransactionDTO> getByClient(@PathVariable Long clientId) {
        return transactionService.getTransactionsByClient(clientId)
                .stream()
                .map(TransactionMapper::toDTO)
                .toList();
    }

    // 4️) Get transactions between two dates
    @GetMapping("/between")
    public List<TransactionDTO> getBetweenDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return transactionService.getTransactionsBetweenDates(from, to)
                .stream()
                .map(TransactionMapper::toDTO)
                .toList();
    }

    // 5️) Get transactions by type code (DEPOSIT, WITHDRAW, TRANSFER)
    @GetMapping("/by-type/{code}")
    public List<TransactionDTO> getByType(@PathVariable String code) {
        return transactionService.getTransactionsByType(code)
                .stream()
                .map(TransactionMapper::toDTO)
                .toList();
    }

    // 6️) Get daily totals (aggregated report)
    @GetMapping("/daily-totals")
    public Map<LocalDate, BigDecimal> getDailyTotals() {
        return transactionService.calculateDailyTotals();
    }
}
