package ro.app.account.controller;

import java.util.Map;

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
import ro.app.account.dto.request.SecurityAccountActionRequest;
import ro.app.account.internal.InternalApiHeaders;
import ro.app.account.service.AccountService;

@RestController
@RequestMapping("/api/internal/accounts/security")
public class InternalSecurityAccountController {

    private final AccountService accountService;
    private final String internalApiSecret;

    public InternalSecurityAccountController(
            AccountService accountService,
            @Value("${app.internal.api-secret}") String internalApiSecret) {
        this.accountService = accountService;
        this.internalApiSecret = internalApiSecret;
    }

    @PostMapping("/freeze")
    public ResponseEntity<Map<String, String>> freeze(
            @RequestHeader(InternalApiHeaders.SECRET) String secret,
            @Valid @RequestBody SecurityAccountActionRequest body) {
        validateSecret(secret);
        accountService.freezeAccount(body.getAccountId());
        return ResponseEntity.ok(Map.of("status", "frozen"));
    }

    @PostMapping("/unfreeze")
    public ResponseEntity<Map<String, String>> unfreeze(
            @RequestHeader(InternalApiHeaders.SECRET) String secret,
            @Valid @RequestBody SecurityAccountActionRequest body) {
        validateSecret(secret);
        accountService.unfreezeAccount(body.getAccountId());
        return ResponseEntity.ok(Map.of("status", "unfrozen"));
    }

    private void validateSecret(String secret) {
        if (!internalApiSecret.equals(secret)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal API secret");
        }
    }
}