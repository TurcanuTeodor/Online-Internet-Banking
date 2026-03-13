package ro.app.payment.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ro.app.payment.dto.PaymentDTO;
import ro.app.payment.dto.request.CreatePaymentRequest;
import ro.app.payment.security.JwtPrincipal;
import ro.app.payment.security.OwnershipChecker;
import ro.app.payment.service.PaymentService;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;
    @Mock
    private OwnershipChecker ownershipChecker;
    @InjectMocks
    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        paymentController = new PaymentController(paymentService, ownershipChecker);
    }

    @Test
    void createPayment_OwnershipCheckCalled() {
        CreatePaymentRequest request = mock(CreatePaymentRequest.class);
        JwtPrincipal principal = mock(JwtPrincipal.class);
        when(request.getClientId()).thenReturn(123L);
        PaymentDTO paymentDTO = mock(PaymentDTO.class);
        when(paymentService.createPayment(request)).thenReturn(paymentDTO);

        ResponseEntity<PaymentDTO> response = paymentController.createPayment(request, principal);

        verify(ownershipChecker).checkOwnership(principal, 123L);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    // Adaugă teste similare pentru celelalte endpointuri cu ownership check
}
