package ro.app.payment.service.payment_method;

import java.util.List;

import org.springframework.stereotype.Service;

import ro.app.payment.dto.PaymentMethodDTO;
import ro.app.payment.dto.request.AttachPaymentMethodRequest;
import ro.app.payment.model.entity.PaymentMethod;
import ro.app.payment.security.JwtPrincipal;
import ro.app.payment.security.OwnershipChecker;
import ro.app.payment.service.payment_method.attachment.PaymentMethodAttachmentService;
import ro.app.payment.service.payment_method.management.PaymentMethodManagementService;
import ro.app.payment.service.payment_method.query.PaymentMethodQueryService;

/**
 * Facade for saved payment methods. Delegates to {@code service.payment_method.attachment}, {@code .query}, {@code .management}.
 */
@Service
public class PaymentMethodService {

    private final PaymentMethodAttachmentService attachmentService;
    private final PaymentMethodQueryService queryService;
    private final PaymentMethodManagementService managementService;
    private final OwnershipChecker ownershipChecker;

    public PaymentMethodService(
            PaymentMethodAttachmentService attachmentService,
            PaymentMethodQueryService queryService,
            PaymentMethodManagementService managementService,
            OwnershipChecker ownershipChecker) {
        this.attachmentService = attachmentService;
        this.queryService = queryService;
        this.managementService = managementService;
        this.ownershipChecker = ownershipChecker;
    }

    public PaymentMethodDTO attachPaymentMethod(AttachPaymentMethodRequest req) {
        return attachmentService.attachPaymentMethod(req);
    }

    public List<PaymentMethodDTO> getByClient(Long clientId) {
        return queryService.getByClient(clientId);
    }

    /**
     * Deletes a saved card after verifying the caller owns the underlying client.
     */
    public void deletePaymentMethod(Long id, JwtPrincipal principal) {
        PaymentMethod pm = queryService.requireById(id);
        ownershipChecker.checkOwnership(principal, pm.getClientId());
        managementService.deletePaymentMethod(pm);
    }

    public PaymentMethodDTO setDefault(Long clientId, Long paymentMethodId) {
        return managementService.setDefault(clientId, paymentMethodId);
    }
}
