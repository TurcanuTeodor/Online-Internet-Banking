package ro.app.account.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.HttpStatus;

import jakarta.validation.Valid;
import ro.app.account.internal.GdprClientIdRequest;
import ro.app.account.internal.InternalApiHeaders;
import ro.app.account.service.AccountGdprInternalService;

@RestController
@RequestMapping("/api/internal/accounts/gdpr")
public class InternalGdprAccountController {

    private final AccountGdprInternalService accountGdprInternalService;
    private final String internalApiSecret;

    public InternalGdprAccountController(
            AccountGdprInternalService accountGdprInternalService,
            @Value("${app.internal.api-secret}") String internalApiSecret) {
        this.accountGdprInternalService = accountGdprInternalService;
        this.internalApiSecret = internalApiSecret;
    }

    @PostMapping("/account-ids")
    public ResponseEntity<List<Long>> accountIds(
            @RequestHeader(InternalApiHeaders.SECRET) String secret,
            @Valid @RequestBody GdprClientIdRequest body) {
        validateSecret(secret);
        return ResponseEntity.ok(accountGdprInternalService.listAccountIdsByClient(body.getClientId()));
    }

    @PostMapping("/close-all")
    public ResponseEntity<Void> closeAll(
            @RequestHeader(InternalApiHeaders.SECRET) String secret,
            @Valid @RequestBody GdprClientIdRequest body) {
        validateSecret(secret);
        accountGdprInternalService.closeAllAccountsForClient(body.getClientId());
        return ResponseEntity.noContent().build();
    }

    private void validateSecret(String secret) {
        if (!internalApiSecret.equals(secret)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal API secret");
        }
    }
}
