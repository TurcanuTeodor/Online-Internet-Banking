package ro.app.fraud.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import ro.app.fraud.dto.FraudEvaluationRequest;
import ro.app.fraud.dto.FraudEvaluationResponse;
import ro.app.fraud.service.FraudService;

@RestController
@RequestMapping("/api/internal/fraud")
public class InternalFraudController {

    private final FraudService fraudService;

    public InternalFraudController(FraudService fraudService) {
        this.fraudService = fraudService;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<FraudEvaluationResponse> evaluate(
            @Valid @RequestBody FraudEvaluationRequest req) {

        return ResponseEntity.ok(fraudService.evaluate(req));
    }
}
