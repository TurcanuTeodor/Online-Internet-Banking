package ro.app.auth.controller;

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
import ro.app.auth.internal.GdprClientIdRequest;
import ro.app.auth.internal.InternalApiHeaders;
import ro.app.auth.service.AuthGdprInternalService;

@RestController
@RequestMapping("/api/internal/auth/gdpr")
public class InternalGdprAuthController {

    private final AuthGdprInternalService authGdprInternalService;
    private final String internalApiSecret;

    public InternalGdprAuthController(
            AuthGdprInternalService authGdprInternalService,
            @Value("${app.internal.api-secret}") String internalApiSecret) {
        this.authGdprInternalService = authGdprInternalService;
        this.internalApiSecret = internalApiSecret;
    }

    @PostMapping("/deactivate-user")
    public ResponseEntity<Void> deactivateUser(
            @RequestHeader(InternalApiHeaders.SECRET) String secret,
            @Valid @RequestBody GdprClientIdRequest body) {
        if (!internalApiSecret.equals(secret)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal API secret");
        }
        authGdprInternalService.deactivateUserForGdpr(body.getClientId());
        return ResponseEntity.noContent().build();
    }
}
