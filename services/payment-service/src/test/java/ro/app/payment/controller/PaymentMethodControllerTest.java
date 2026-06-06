package ro.app.payment.controller;

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

import ro.app.payment.dto.PaymentMethodDTO;
import ro.app.payment.dto.request.AttachPaymentMethodRequest;
import ro.app.payment.security.JwtPrincipal;
import ro.app.payment.security.OwnershipChecker;
import ro.app.payment.service.payment_method.PaymentMethodService;

/**
 * Teste unitare pentru PaymentMethodController (JUnit 4).
 *
 * Pattern testat: Facade (Structural) — controllerul verifica ownership
 * si delega catre PaymentMethodService.
 *
 * DTO-urile si JwtPrincipal se construiesc cu "new" (nu mock).
 */
@RunWith(MockitoJUnitRunner.class)
public class PaymentMethodControllerTest {

    @Mock
    private PaymentMethodService paymentMethodService;

    @Mock
    private OwnershipChecker ownershipChecker;

    @InjectMocks
    private PaymentMethodController paymentMethodController;

    private JwtPrincipal clientPrincipal;

    @Before
    public void setUp() {
        clientPrincipal = new JwtPrincipal("ion.pop", 123L, "CLIENT");
    }

    // ── attach ────────────────────────────────────────────────────────────────

    @Test
    public void attach_ownershipCheckCalled_withClientId() {
        // Arrange
        AttachPaymentMethodRequest request = buildRequest(123L, "pm_test_visa_4242");
        Mockito.when(paymentMethodService.attachPaymentMethod(request)).thenReturn(new PaymentMethodDTO());

        // Act
        paymentMethodController.attach(request, clientPrincipal);

        // Assert
        Mockito.verify(ownershipChecker).checkOwnership(clientPrincipal, 123L);
    }

    @Test
    public void attach_validRequest_returnsCreated() {
        // Arrange
        AttachPaymentMethodRequest request = buildRequest(123L, "pm_test_mastercard_5555");
        PaymentMethodDTO dto = new PaymentMethodDTO();
        Mockito.when(paymentMethodService.attachPaymentMethod(request)).thenReturn(dto);

        // Act
        ResponseEntity<PaymentMethodDTO> response = paymentMethodController.attach(request, clientPrincipal);

        // Assert
        Assert.assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Assert.assertNotNull(response.getBody());
    }

    @Test
    public void attach_delegatesToService() {
        // Arrange
        AttachPaymentMethodRequest request = buildRequest(123L, "pm_test_visa_4242");
        Mockito.when(paymentMethodService.attachPaymentMethod(request)).thenReturn(new PaymentMethodDTO());

        // Act
        paymentMethodController.attach(request, clientPrincipal);

        // Assert
        Mockito.verify(paymentMethodService).attachPaymentMethod(request);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AttachPaymentMethodRequest buildRequest(Long clientId, String stripeId) {
        AttachPaymentMethodRequest req = new AttachPaymentMethodRequest();
        req.setClientId(clientId);
        req.setStripePaymentMethodId(stripeId);
        return req;
    }
}
