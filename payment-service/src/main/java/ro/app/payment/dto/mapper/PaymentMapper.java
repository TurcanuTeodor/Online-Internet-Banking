package ro.app.payment.dto.mapper;

import ro.app.payment.dto.PaymentDTO;
import ro.app.payment.dto.request.CreatePaymentRequest;
import ro.app.payment.model.entity.Payment;
import ro.app.payment.model.enums.CurrencyType;
import ro.app.payment.model.enums.PaymentStatus;

public class PaymentMapper {
    
    private PaymentMapper(){}

    public static PaymentDTO toDTO(Payment e){
        PaymentDTO dto = new PaymentDTO();

        dto.setId(e.getId());
        dto.setClientId(e.getClientId());
        dto.setAccountId(e.getAccountId());
        dto.setAmount(e.getAmount());
        dto.setCurrencyCode(e.getCurrency() != null ? e.getCurrency().getCode() : null);
        dto.setStatus(e.getStatus() != null ? e.getStatus().getCode() : null);
        dto.setStripePaymentIntentId(e.getStripePaymentIntentId());
        dto.setDescription(e.getDescription());
        dto.setCreatedAt(e.getCreatedAt());

        return dto;
    }

    public static Payment toEntity(CreatePaymentRequest req){
        Payment e= new Payment();

        e.setClientId(req.getClientId());
        e.setAccountId(req.getAccountId());
        e.setAmount(req.getAmount());
        e.setCurrency(CurrencyType.fromCode(req.getCurrencyCode()));
        e.setStatus(PaymentStatus.PENDING);
        e.setDescription(req.getDescription());

        //stripePaymentIntentId is set later after Stripe API call

        return e;

    }
}
