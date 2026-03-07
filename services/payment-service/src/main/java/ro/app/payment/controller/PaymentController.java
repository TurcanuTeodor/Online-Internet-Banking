package ro.app.payment.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import ro.app.payment.dto.PaymentDTO;
import ro.app.payment.dto.request.CreatePaymentRequest;
import ro.app.payment.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
@Validated
public class PaymentController {
    
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService){
        this.paymentService = paymentService;
    }

    //Create a new payment => Stripe payment intent
    @PostMapping
    public ResponseEntity<PaymentDTO> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        PaymentDTO result = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
   
    // Get payment by ID
    @GetMapping("/{id}")
    public PaymentDTO getById(@PathVariable Long id) {
        return paymentService.getById(id);
    }

    // Refund a payment
    @PostMapping("/{id}/refund")
    public PaymentDTO refundPayment(@PathVariable Long id) {
        return paymentService.refundPayment(id);
    }

    // Get all payments for a client
    @GetMapping("/by-client/{clientId}")
    public List<PaymentDTO> getByClient(@PathVariable Long clientId) {
        return paymentService.getPaymentsByClient(clientId);
    }

}
