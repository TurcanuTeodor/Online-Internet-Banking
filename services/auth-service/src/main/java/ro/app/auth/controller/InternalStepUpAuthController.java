package ro.app.auth.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

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
import ro.app.auth.internal.InternalApiHeaders;
import ro.app.auth.internal.StepUpRequest;
import ro.app.auth.service.TwoFaService;

@RestController
@RequestMapping("/api/internal/auth")
public class InternalStepUpAuthController {

    private final TwoFaService twoFaService;
    private final String internalApiSecret;

    public InternalStepUpAuthController(
            TwoFaService twoFaService,
            @Value("${app.internal.api-secret}") String internalApiSecret) {
        this.twoFaService = twoFaService;
        this.internalApiSecret = internalApiSecret;
    }

    @PostMapping("/step-up")
    public ResponseEntity<Void> verifyStepUp(
            @RequestHeader(InternalApiHeaders.SECRET) String secret,
            @Valid @RequestBody StepUpRequest request) {
        if (!MessageDigest.isEqual(
                internalApiSecret.getBytes(StandardCharsets.UTF_8),
                secret.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal API secret");
        }

        twoFaService.verifyStepUp(request.clientId(), request.totpCode());
        return ResponseEntity.ok().build();
    }
}
