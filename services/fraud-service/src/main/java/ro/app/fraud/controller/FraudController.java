package ro.app.fraud.controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import ro.app.fraud.dto.FraudDecisionDTO;
import ro.app.fraud.model.enums.FraudDecisionStatus;
import ro.app.fraud.security.JwtPrincipal;
import ro.app.fraud.service.FraudService;

@RestController
@RequestMapping("/api/fraud")
public class FraudController {

    private final FraudService fraudService;

    public FraudController(FraudService fraudService) {
        this.fraudService = fraudService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "fraud-service"));
    }

    @GetMapping("/decisions/{id}")
    public ResponseEntity<FraudDecisionDTO> getDecision(@PathVariable Long id) {
        return ResponseEntity.ok(fraudService.getDecision(id));
    }

    @GetMapping("/decisions/by-transaction/{transactionId}")
    public ResponseEntity<FraudDecisionDTO> getByTransaction(@PathVariable Long transactionId) {
        return ResponseEntity.ok(fraudService.getByTransactionId(transactionId));
    }

    @GetMapping("/alerts")
    public ResponseEntity<Page<FraudDecisionDTO>> getAlerts(Pageable pageable) {
        return ResponseEntity.ok(fraudService.getAlerts(pageable));
    }

    @PutMapping("/decisions/{id}/review")
    public ResponseEntity<FraudDecisionDTO> adminReview(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal JwtPrincipal principal) {

        FraudDecisionStatus newStatus = FraudDecisionStatus.valueOf(body.getOrDefault("status", "ALLOW"));
        String notes = body.getOrDefault("notes", "");

        return ResponseEntity.ok(fraudService.adminReview(id, principal.username(), notes, newStatus));
    }
}
