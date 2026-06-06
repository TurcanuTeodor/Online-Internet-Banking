package ro.app.payment.controller;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ro.app.payment.dto.PaymentDTO;
import ro.app.payment.dto.request.CreatePaymentRequest;
import ro.app.payment.security.JwtPrincipal;
import ro.app.payment.security.OwnershipChecker;
import ro.app.payment.service.payment.PaymentService;

/**
 * Teste unitare pentru PaymentController (JUnit 4).
 *
 * Pattern testat: Facade (Structural) — controllerul verifica ownership
 * si delega catre PaymentService.
 *
 * DTO-urile si JwtPrincipal se construiesc cu "new" (nu mock).
 */
@RunWith(MockitoJUnitRunner.class)
public class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private OwnershipChecker ownershipChecker;

    @InjectMocks
    private PaymentController paymentController;

    private JwtPrincipal clientPrincipal;

    @Before
    public void setUp() {
        clientPrincipal = new JwtPrincipal("ion.pop", 123L, "CLIENT");
    }

    // ── createPayment ─────────────────────────────────────────────────────────

    @Test
    public void createPayment_ownershipCheckCalled_withClientId() {
        // Arrange
        CreatePaymentRequest request = buildRequest(123L, BigDecimal.valueOf(100));
        PaymentDTO paymentDTO = new PaymentDTO();
        Mockito.when(paymentService.createPayment(request)).thenReturn(paymentDTO);

        // Act
        paymentController.createPayment(request, clientPrincipal);

        // Assert
        Mockito.verify(ownershipChecker).checkOwnership(clientPrincipal, 123L);
    }

    @Test
    public void createPayment_validRequest_returnsCreated() {
        // Arrange
        CreatePaymentRequest request = buildRequest(123L, BigDecimal.valueOf(50));
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setClientId(123L);
        Mockito.when(paymentService.createPayment(request)).thenReturn(paymentDTO);

        // Act
        ResponseEntity<PaymentDTO> response = paymentController.createPayment(request, clientPrincipal);

        // Assert
        Assert.assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Assert.assertNotNull(response.getBody());
    }

    @Test
    public void createPayment_delegatesToPaymentService() {
        // Arrange
        CreatePaymentRequest request = buildRequest(123L, BigDecimal.valueOf(200));
        Mockito.when(paymentService.createPayment(request)).thenReturn(new PaymentDTO());

        // Act
        paymentController.createPayment(request, clientPrincipal);

        // Assert
        Mockito.verify(paymentService).createPayment(request);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CreatePaymentRequest buildRequest(Long clientId, BigDecimal amount) {
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setClientId(clientId);
        req.setAccountId(10L);
        req.setAmount(amount);
        req.setCurrencyCode("EUR");
        req.setPaymentMethodId("pm_test_visa_4242");
        return req;
    }
}
