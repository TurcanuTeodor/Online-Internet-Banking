package ro.app.payment.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import ro.app.payment.dto.PaymentDTO;
import ro.app.payment.dto.request.CreatePaymentRequest;
import ro.app.payment.dto.request.CreateTopUpIntentRequest;
import ro.app.payment.dto.response.TopUpIntentResponse;
import ro.app.payment.service.PaymentService;
import ro.app.payment.security.JwtPrincipal;
import ro.app.payment.security.OwnershipChecker;

@RestController
@RequestMapping("/api/payments")
@Validated
public class PaymentController {
    
    private final PaymentService paymentService;
    private final OwnershipChecker ownershipChecker;

    public PaymentController(PaymentService paymentService, OwnershipChecker ownershipChecker){
        this.paymentService = paymentService;
        this.ownershipChecker = ownershipChecker;
    }

    /**
     * One-time card top-up: returns Stripe {@code clientSecret} for {@code confirmCardPayment} with Elements.
     */
    @PostMapping("/top-up/intent")
    public ResponseEntity<TopUpIntentResponse> createTopUpIntent(
            @Valid @RequestBody CreateTopUpIntentRequest request,
            @AuthenticationPrincipal JwtPrincipal principal,
            HttpServletRequest httpRequest) {
        String authorization = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
        TopUpIntentResponse body = paymentService.createTopUpIntent(request, principal, authorization);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // Create payment — ownership check pe clientId din request
    @PostMapping
    public ResponseEntity<PaymentDTO> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal JwtPrincipal principal) {
        ownershipChecker.checkOwnership(principal, request.getClientId());
        PaymentDTO result = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
   
    // Get payment by ID — ownership check pe clientId din payment
    @GetMapping("/{id}")
    public PaymentDTO getById(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtPrincipal principal) {
        PaymentDTO payment = paymentService.getById(id);
        ownershipChecker.checkOwnership(principal, payment.getClientId());
        return payment;
    }

   // Refund — ownership check pe clientId din payment
    @PostMapping("/{id}/refund")
    public PaymentDTO refundPayment(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtPrincipal principal) {
        PaymentDTO payment = paymentService.getById(id);
        ownershipChecker.checkOwnership(principal, payment.getClientId());
        return paymentService.refundPayment(id);
    }

    // Get all payments for a client — ownership check pe clientId din path
    @GetMapping("/by-client/{clientId}")
    public List<PaymentDTO> getByClient(
            @PathVariable Long clientId,
            @AuthenticationPrincipal JwtPrincipal principal) {
        ownershipChecker.checkOwnership(principal, clientId);
        return paymentService.getPaymentsByClient(clientId);
    }

}
