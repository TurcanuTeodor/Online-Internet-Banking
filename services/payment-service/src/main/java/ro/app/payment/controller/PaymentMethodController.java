package ro.app.payment.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import ro.app.payment.dto.PaymentMethodDTO;
import ro.app.payment.dto.request.AttachPaymentMethodRequest;
import ro.app.payment.service.payment_method.PaymentMethodService;
import ro.app.payment.security.JwtPrincipal;
import ro.app.payment.security.OwnershipChecker;

@RestController
@RequestMapping("/api/payment-methods")
@Validated
public class PaymentMethodController {
    
    private final PaymentMethodService paymentMethodService;
    private final OwnershipChecker ownershipChecker;

    public PaymentMethodController(PaymentMethodService paymentMethodService, OwnershipChecker ownershipChecker) {
        this.paymentMethodService = paymentMethodService;
        this.ownershipChecker = ownershipChecker;
    }

    // Attach card — ownership check pe clientId din request
    @PostMapping
    public ResponseEntity<PaymentMethodDTO> attach(
            @Valid @RequestBody AttachPaymentMethodRequest request,
            @AuthenticationPrincipal JwtPrincipal principal) {
        ownershipChecker.checkOwnership(principal, request.getClientId());
        PaymentMethodDTO result = paymentMethodService.attachPaymentMethod(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // Get cards for client — ownership check pe clientId din path
    @GetMapping("/by-client/{clientId}")
    public List<PaymentMethodDTO> getByClient(
            @PathVariable Long clientId,
            @AuthenticationPrincipal JwtPrincipal principal) {
        ownershipChecker.checkOwnership(principal, clientId);
        return paymentMethodService.getByClient(clientId);
    }

    // Delete card — ownership via clientId on stored PaymentMethod
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtPrincipal principal) {
        paymentMethodService.deletePaymentMethod(id, principal);
        return ResponseEntity.noContent().build();
    }

    // Set default — ownership check pe clientId din request
    @PutMapping("/{id}/set-default")
    public PaymentMethodDTO setDefault(
            @PathVariable Long id,
            @RequestParam Long clientId,
            @AuthenticationPrincipal JwtPrincipal principal) {
        ownershipChecker.checkOwnership(principal, clientId);
        return paymentMethodService.setDefault(clientId, id);
    }

}