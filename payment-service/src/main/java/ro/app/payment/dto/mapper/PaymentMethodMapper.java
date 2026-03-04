package ro.app.payment.dto.mapper;

import ro.app.payment.dto.PaymentMethodDTO;
import ro.app.payment.model.entity.PaymentMethod;

public class PaymentMethodMapper {
    
    private PaymentMethodMapper() {}

    public static PaymentMethodDTO toDTO(PaymentMethod e){
        PaymentMethodDTO dto = new PaymentMethodDTO();

        dto.setId(e.getId());
        dto.setClientId(e.getClientId());
        dto.setStripePaymentMethodId(e.getStripePaymentMethodId());
        dto.setCardBrand(e.getCardBrand());
        dto.setCardLast4(e.getCardLast4());
        dto.setExpiryMonth(e.getExpiryMonth());
        dto.setExpiryYear(e.getExpiryYear());
        dto.setIsDefault(e.getIsDefault());
        dto.setCreatedAt(e.getCreatedAt());
        return dto;
    }

    //No toEntity - entity is built manually in service after Stripe API returns card details

}
