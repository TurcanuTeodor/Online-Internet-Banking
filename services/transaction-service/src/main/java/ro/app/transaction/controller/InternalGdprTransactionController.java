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
import ro.app.transaction.internal.GdprAccountIdsRequest;
import ro.app.transaction.internal.InternalApiHeaders;
import ro.app.transaction.service.TransactionService;

@RestController
@RequestMapping("/api/internal/transactions/gdpr")
public class InternalGdprTransactionController {

    private static final String ANONYMIZED = "ANONYMIZED";

    private final TransactionService transactionService;
    private final String internalApiSecret;

    public InternalGdprTransactionController(
            TransactionService transactionService,
            @Value("${app.internal.api-secret}") String internalApiSecret) {
        this.transactionService = transactionService;
        this.internalApiSecret = internalApiSecret;
    }

    @PostMapping("/anonymize-details")
    public ResponseEntity<Void> anonymizeDetails(
            @RequestHeader(InternalApiHeaders.SECRET) String secret,
            @Valid @RequestBody GdprAccountIdsRequest body) {
        if (!internalApiSecret.equals(secret)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal API secret");
        }
        transactionService.anonymizeDetailsForAccountIds(body.getAccountIds(), ANONYMIZED);
        return ResponseEntity.noContent().build();
    }
}
