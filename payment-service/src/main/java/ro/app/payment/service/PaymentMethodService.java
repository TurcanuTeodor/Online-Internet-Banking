package ro.app.payment.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.stripe.exception.StripeException;

import ro.app.payment.dto.PaymentMethodDTO;
import ro.app.payment.dto.mapper.PaymentMethodMapper;
import ro.app.payment.dto.request.AttachPaymentMethodRequest;
import ro.app.payment.exception.PaymentFailedException;
import ro.app.payment.exception.ResourceNotFoundException;
import ro.app.payment.model.entity.PaymentMethod;
import ro.app.payment.repository.PaymentMethodRepository;

@Service
public class PaymentMethodService {
    
    private static final Logger log = LoggerFactory.getLogger(PaymentMethodService.class);
    private final PaymentMethodRepository paymentMethodRepository;

    public PaymentMethodService(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    //---Attch a payment method -> retrieve card details from Stripe , save locally ----
    public PaymentMethodDTO attachPaymentMethod(AttachPaymentMethodRequest req){
        try{
            //Retrieve card details from Stripe 
            com.stripe.model.PaymentMethod stripePm= 
                com.stripe.model.PaymentMethod.retrieve(req.getStripePaymentMethodId());

            PaymentMethod entity = new PaymentMethod();
            entity.setClientId(req.getClientId());
            entity.setStripePaymentMethodId(req.getStripePaymentMethodId());

            //Exact card details from Stripe response
            if(stripePm.getCard() != null){
                entity.setCardBrand(stripePm.getCard().getBrand());
                entity.setCardLast4(stripePm.getCard().getLast4());
                entity.setExpiryMonth(stripePm.getCard().getExpMonth().intValue());
                entity.setExpiryYear(stripePm.getCard().getExpYear().intValue());
            }

            //1st card for this client becomes deafault
            boolean hasExisting = !paymentMethodRepository
                .findByClientIdOrderByCreatedAtDesc(req.getClientId()).isEmpty();
            entity.setIsDefault(!hasExisting);

            entity= paymentMethodRepository.save(entity);
            return PaymentMethodMapper.toDTO(entity);

        } catch (StripeException e) {
            log.error("Failed to retrieve payment method from Stripe: {}", e.getMessage());
            throw new PaymentFailedException("Failed to attach payment method: " + e.getMessage(), e);
        }
    }

    //----Get ALL for a client---
    public List<PaymentMethodDTO> getByClient(Long clientId){
        return paymentMethodRepository.findByClientIdOrderByCreatedAtDesc(clientId)
            .stream()
            .map(PaymentMethodMapper::toDTO)
            .toList();
    }

    //---Delete a payment method----
    public void deletePaymentMethod(Long id){
        PaymentMethod entity = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment method not found with id: " + id));
    
        try{
            //detach from Stripe
            com.stripe.model.PaymentMethod stripePm=
                com.stripe.model.PaymentMethod.retrieve(entity.getStripePaymentMethodId());
            stripePm.detach();

        }catch(StripeException e) {
            log.warn("Failed to detach payment method from Stripe (may already be detached): {}", e.getMessage());
        }

        paymentMethodRepository.delete(entity);
    }

    //---Set a payment method as default----
    public PaymentMethodDTO setDefault(Long clientId, Long paymentMethodId) {
        // Remove default from all client's cards
        List<PaymentMethod> allCards = paymentMethodRepository.findByClientIdOrderByCreatedAtDesc(clientId);
        for (PaymentMethod card : allCards) {
            if (card.getIsDefault()) {
                card.setIsDefault(false);
                paymentMethodRepository.save(card);
            }
        }

        // Set new default
        PaymentMethod target = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment method not found with id: " + paymentMethodId));

        target.setIsDefault(true);
        target = paymentMethodRepository.save(target);
        return PaymentMethodMapper.toDTO(target);
    }
}
