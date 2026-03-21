package ro.app.transaction.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import ro.app.transaction.dto.TransactionDTO;
import ro.app.transaction.dto.mapper.TransactionMapper;
import ro.app.transaction.model.entity.Transaction;
import ro.app.transaction.service.TransactionService;

/**
 * Server-to-server deposit recording (e.g. Stripe top-up). Protected by shared secret, not JWT.
 */
@RestController
@RequestMapping("/api/internal/transactions")
public class InternalTransactionController {

    public static final String SECRET_HEADER = "X-Internal-Api-Secret";

    private final TransactionService transactionService;
    private final String internalApiSecret;

    public InternalTransactionController(
            TransactionService transactionService,
            @Value("${app.internal.api-secret}") String internalApiSecret) {
        this.transactionService = transactionService;
        this.internalApiSecret = internalApiSecret;
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionDTO> recordDeposit(
            @RequestHeader(SECRET_HEADER) String secret,
            @Valid @RequestBody TransactionDTO dto) {
        if (!internalApiSecret.equals(secret)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal API secret");
        }
        if (!"DEPOSIT".equalsIgnoreCase(dto.getTransactionTypeCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only DEPOSIT is allowed on this endpoint");
        }
        Transaction entity = TransactionMapper.toEntity(dto);
        Transaction saved = transactionService.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionMapper.toDTO(saved));
    }
}
