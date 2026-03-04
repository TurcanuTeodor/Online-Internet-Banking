package ro.app.payment.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
 
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;

import ro.app.payment.dto.PaymentDTO;
import ro.app.payment.dto.mapper.PaymentMapper;
import ro.app.payment.dto.request.CreatePaymentRequest;
import ro.app.payment.exception.PaymentFailedException;
import ro.app.payment.exception.ResourceNotFoundException;
import ro.app.payment.model.entity.Payment;
import ro.app.payment.model.enums.PaymentStatus;
import ro.app.payment.repository.PaymentRepository;

@Service
public class PaymentService {

    private static final Logger log= LoggerFactory.getLogger(PaymentService.class);
    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository){
        this.paymentRepository = paymentRepository;
    } 

    //---- Create a Stripe PaymentIntent and persist locally-----
    public PaymentDTO createPayment(CreatePaymentRequest req){
        Payment payment = PaymentMapper.toEntity(req);

        try{
            //Bc Stripe expects amount in cents
            long amountInCents= req.getAmount().movePointRight(2).longValueExact();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(req.getCurrencyCode().toLowerCase())
                .setPaymentMethod(req.getPaymentMethodId())
                .setConfirm(true)
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                        .build()
                )
                .build();

            PaymentIntent intent = PaymentIntent.create(params);

            payment.setStripePaymentIntentId(intent.getId());

            //Map Stripe status to my enums
            if("succeeded".equals(intent.getStatus())){
                payment.setStatus(PaymentStatus.COMPLETED);
            }
            else{
                payment.setStatus(PaymentStatus.PENDING);
            }

        }catch(StripeException e){
            log.error("Stripe PaymentIntent creation failed: {}", e.getMessage());
            payment.setStatus(PaymentStatus.FAILED);
            payment = paymentRepository.save(payment);
            throw new PaymentFailedException("Payment failed: " + e.getMessage(), e);
        }

        payment = paymentRepository.save(payment);
        return PaymentMapper.toDTO(payment);
    }

    //---Get payment by id----
    public PaymentDTO getById(Long id){
        Payment payment = findPaymentOrThrow(id);
        return PaymentMapper.toDTO(payment);
    }

    //---Get ALL for a client---
    public List<PaymentDTO> getPaymentsByClient(Long clientId){
        return paymentRepository.findByClientIdOrderByCreatedAtDesc(clientId)
                .stream()
                .map(PaymentMapper::toDTO)
                .toList();
    }

    //----Refund a completed payment----
    public PaymentDTO refundPayment(Long id){
        Payment payment = findPaymentOrThrow(id);

        if(payment.getStatus() != PaymentStatus.COMPLETED){
            throw new PaymentFailedException("Only completed payments can be refunded. Current status: " + payment.getStatus());
        }

        try{
            RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(payment.getStripePaymentIntentId())
                .build();

            Refund.create(params);

            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setUpdatedAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);
        }catch(StripeException e){
            log.error("Strip refund failed for payment {}: {}", id, e.getMessage());
            throw new PaymentFailedException("Refund failed: "+ e.getMessage(), e);
        }

        return PaymentMapper.toDTO(payment);
    }

    //----Handle Stripe Webhook events----
    public void handleWebhookEvent(String eventType , String paymentIntentId){
        Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElse(null);

        if(payment == null){
            log.warn("Webhook: no payment found for intent {}", paymentIntentId);
            return;
        }

        switch(eventType){
            case "payment_intent.succeeded" -> payment.setStatus(PaymentStatus.COMPLETED);
            case "payment_intent.payment_failed" -> payment.setStatus(PaymentStatus.FAILED);
            case "charge.refunded" -> payment.setStatus(PaymentStatus.REFUNDED);
            default -> {
                log.info("Webhook: unhandled event type {}", eventType);
                return;
            }
        }

        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        log.info("Webhook: payment {} updated to {}", paymentIntentId, payment.getStatus());

    }

    //Helper
    private Payment findPaymentOrThrow(Long id){
        return paymentRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Payment not found with id: " + id));
    }

} 
