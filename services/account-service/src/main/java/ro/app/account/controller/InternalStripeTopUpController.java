package ro.app.account.controller;

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
import ro.app.account.dto.request.StripeTopUpApplyRequest;
import ro.app.account.internal.InternalApiHeaders;
import ro.app.account.service.AccountService;

/**
 * Server-to-server only: payment-service calls this after Stripe webhook confirms funds.
 * Protected by a shared secret header (not JWT).
 */
@RestController
@RequestMapping("/api/internal/stripe-top-up")
public class InternalStripeTopUpController {

    private final AccountService accountService;
    private final String internalApiSecret;

    public InternalStripeTopUpController(
            AccountService accountService,
            @Value("${app.internal.api-secret}") String internalApiSecret) {
        this.accountService = accountService;
        this.internalApiSecret = internalApiSecret;
    }

    @PostMapping("/apply")
    public ResponseEntity<Void> apply(
            @RequestHeader(InternalApiHeaders.SECRET) String secret,
            @Valid @RequestBody StripeTopUpApplyRequest body) {
        if (!internalApiSecret.equals(secret)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal API secret");
        }
        accountService.applyStripeTopUpCredit(body);
        return ResponseEntity.ok().build();
    }
}
