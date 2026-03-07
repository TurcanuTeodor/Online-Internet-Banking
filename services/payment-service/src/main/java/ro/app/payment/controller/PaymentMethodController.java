package ro.app.payment.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import ro.app.payment.dto.PaymentMethodDTO;
import ro.app.payment.dto.request.AttachPaymentMethodRequest;
import ro.app.payment.service.PaymentMethodService;

@RestController
@RequestMapping("/api/payment-methods")
@Validated
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    public PaymentMethodController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    // Attach a new card (from Stripe token)
    @PostMapping
    public ResponseEntity<PaymentMethodDTO> attach(@Valid @RequestBody AttachPaymentMethodRequest request) {
        PaymentMethodDTO result = paymentMethodService.attachPaymentMethod(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // Get all cards for a client
    @GetMapping("/by-client/{clientId}")
    public List<PaymentMethodDTO> getByClient(@PathVariable Long clientId) {
        return paymentMethodService.getByClient(clientId);
    }

    // Delete a card
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        paymentMethodService.deletePaymentMethod(id);
        return ResponseEntity.noContent().build();
    }

    // Set a card as default
    @PutMapping("/{id}/set-default")
    public PaymentMethodDTO setDefault(@PathVariable Long id, @RequestParam Long clientId) {
        return paymentMethodService.setDefault(clientId, id);
    }
}