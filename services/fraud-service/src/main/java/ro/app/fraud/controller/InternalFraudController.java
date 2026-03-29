package ro.app.fraud.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import ro.app.fraud.dto.FraudEvaluationRequest;
import ro.app.fraud.dto.FraudEvaluationResponse;
import ro.app.fraud.internal.InternalApiHeaders;
import ro.app.fraud.service.FraudService;

@RestController
@RequestMapping("/api/internal/fraud")
public class InternalFraudController {

    private final FraudService fraudService;
    private final String internalSecret;

    public InternalFraudController(FraudService fraudService,
                                   @Value("${app.internal.api-secret}") String internalSecret) {
        this.fraudService = fraudService;
        this.internalSecret = internalSecret;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<FraudEvaluationResponse> evaluate(
            @RequestHeader(InternalApiHeaders.SECRET_HEADER) String secret,
            @Valid @RequestBody FraudEvaluationRequest req) {

        if (!internalSecret.equals(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(fraudService.evaluate(req));
    }
}
