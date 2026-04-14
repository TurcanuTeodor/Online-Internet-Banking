package ro.app.payment.service.payment;

import java.util.List;

import org.springframework.stereotype.Service;

import ro.app.payment.dto.PaymentDTO;
import ro.app.payment.dto.request.CreatePaymentRequest;
import ro.app.payment.dto.request.CreateTopUpIntentRequest;
import ro.app.payment.dto.response.TopUpIntentResponse;
import ro.app.payment.security.JwtPrincipal;
import ro.app.payment.service.payment.creation.PaymentCreationService;
import ro.app.payment.service.payment.query.PaymentQueryService;
import ro.app.payment.service.payment.refund.PaymentRefundService;
import ro.app.payment.service.payment.webhook.PaymentWebhookService;
import ro.app.payment.service.payment.settlement.PaymentSettlementService;

/**
 * Facade for payment operations. Delegates to {@code service.payment.creation}, {@code service.payment.query}, etc.
 */
@Service
public class PaymentService {

    private final PaymentCreationService paymentCreationService;
    private final PaymentQueryService paymentQueryService;
    private final PaymentRefundService paymentRefundService;
    private final PaymentWebhookService paymentWebhookService;
    private final PaymentSettlementService paymentSettlementService;

    public PaymentService(
            PaymentCreationService paymentCreationService,
            PaymentQueryService paymentQueryService,
            PaymentRefundService paymentRefundService,
            PaymentWebhookService paymentWebhookService,
            PaymentSettlementService paymentSettlementService) {
        this.paymentCreationService = paymentCreationService;
        this.paymentQueryService = paymentQueryService;
        this.paymentRefundService = paymentRefundService;
        this.paymentWebhookService = paymentWebhookService;
        this.paymentSettlementService = paymentSettlementService;
    }

    public TopUpIntentResponse createTopUpIntent(
            CreateTopUpIntentRequest req,
            JwtPrincipal principal,
            String authorizationHeader) {
        return paymentCreationService.createTopUpIntent(req, principal, authorizationHeader);
    }

    public PaymentDTO createPayment(CreatePaymentRequest req) {
        return paymentCreationService.createPayment(req);
    }

    public PaymentDTO getById(Long id) {
        return paymentQueryService.getById(id);
    }

    public List<PaymentDTO> getPaymentsByClient(Long clientId) {
        return paymentQueryService.getPaymentsByClient(clientId);
    }

    public PaymentDTO refundPayment(Long id) {
        return paymentRefundService.refundPayment(id);
    }

    public void handleWebhookEvent(String eventType, String paymentIntentId) {
        paymentWebhookService.handleWebhookEvent(eventType, paymentIntentId);
    }

    public void confirmTopUpSucceeded(String stripePaymentIntentId, JwtPrincipal principal) {
        paymentSettlementService.confirmTopUpSucceeded(stripePaymentIntentId, principal);
    }
}
