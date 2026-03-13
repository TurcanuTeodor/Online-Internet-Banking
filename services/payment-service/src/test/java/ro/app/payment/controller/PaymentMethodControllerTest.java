package ro.app.payment.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ro.app.payment.dto.PaymentMethodDTO;
import ro.app.payment.dto.request.AttachPaymentMethodRequest;
import ro.app.payment.security.JwtPrincipal;
import ro.app.payment.security.OwnershipChecker;
import ro.app.payment.service.PaymentMethodService;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class PaymentMethodControllerTest {

    @Mock
    private PaymentMethodService paymentMethodService;
    @Mock
    private OwnershipChecker ownershipChecker;
    @InjectMocks
    private PaymentMethodController paymentMethodController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        paymentMethodController = new PaymentMethodController(paymentMethodService, ownershipChecker);
    }

    @Test
    void attach_OwnershipCheckCalled() {
        AttachPaymentMethodRequest request = mock(AttachPaymentMethodRequest.class);
        JwtPrincipal principal = mock(JwtPrincipal.class);
        when(request.getClientId()).thenReturn(123L);
        PaymentMethodDTO dto = mock(PaymentMethodDTO.class);
        when(paymentMethodService.attachPaymentMethod(request)).thenReturn(dto);

        ResponseEntity<PaymentMethodDTO> response = paymentMethodController.attach(request, principal);

        verify(ownershipChecker).checkOwnership(principal, 123L);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    // Adaugă teste similare pentru celelalte endpointuri cu ownership check
}
