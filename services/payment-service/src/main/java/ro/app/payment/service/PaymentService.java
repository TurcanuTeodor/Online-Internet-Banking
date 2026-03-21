package ro.app.payment.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;

import ro.app.payment.client.AccountRestClient;
import ro.app.payment.dto.ExternalAccountDto;
import ro.app.payment.dto.PaymentDTO;
import ro.app.payment.dto.mapper.PaymentMapper;
import ro.app.payment.dto.request.CreatePaymentRequest;
import ro.app.payment.dto.request.CreateTopUpIntentRequest;
import ro.app.payment.dto.response.TopUpIntentResponse;
import ro.app.payment.exception.PaymentFailedException;
import ro.app.payment.exception.ResourceNotFoundException;
import ro.app.payment.model.entity.Payment;
import ro.app.payment.model.enums.CurrencyType;
import ro.app.payment.model.enums.PaymentStatus;
import ro.app.payment.repository.PaymentRepository;
import ro.app.payment.security.JwtPrincipal;
import ro.app.payment.security.OwnershipChecker;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private static final Set<String> TOP_UP_STRIPE_CURRENCIES = Set.of("EUR", "RON");

    private final PaymentRepository paymentRepository;
    private final AccountRestClient accountRestClient;
    private final RestTemplate restTemplate;
    private final OwnershipChecker ownershipChecker;

    @Value("${app.services.account.url}")
    private String accountServiceBaseUrl;

    @Value("${app.internal.api-secret}")
    private String internalApiSecret;

    public PaymentService(
            PaymentRepository paymentRepository,
            AccountRestClient accountRestClient,
            RestTemplate restTemplate,
            OwnershipChecker ownershipChecker) {
        this.paymentRepository = paymentRepository;
        this.accountRestClient = accountRestClient;
        this.restTemplate = restTemplate;
        this.ownershipChecker = ownershipChecker;
    }

    /**
     * Card top-up: create local {@link Payment} row + Stripe PaymentIntent (unconfirmed).
     * Currency comes from the account; user pays with {@code CardElement} on the client (no saved PM).
     */
    public TopUpIntentResponse createTopUpIntent(
            CreateTopUpIntentRequest req,
            JwtPrincipal principal,
            String authorizationHeader) {

        ExternalAccountDto account = accountRestClient.getAccountById(req.getAccountId(), authorizationHeader);
        if (account == null || account.getId() == null) {
            throw new ResourceNotFoundException("Account not found");
        }

        ownershipChecker.checkOwnership(principal, account.getClientId());

        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new IllegalArgumentException("Only ACTIVE accounts can receive top-ups");
        }

        String currencyCode = account.getCurrencyCode();
        if (currencyCode == null || !TOP_UP_STRIPE_CURRENCIES.contains(currencyCode.toUpperCase())) {
            throw new IllegalArgumentException("Top-up is only supported for EUR and RON accounts");
        }

        CurrencyType paymentCurrency = CurrencyType.fromCode(currencyCode);
        BigDecimal amount = req.getAmount().setScale(2, java.math.RoundingMode.HALF_UP);

        Payment payment = new Payment();
        payment.setClientId(account.getClientId());
        payment.setAccountId(account.getId());
        payment.setAmount(amount);
        payment.setCurrency(paymentCurrency);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setDescription("Card top-up (one-time)");
        payment = paymentRepository.save(payment);

        try {
            long amountInMinorUnits = amount.movePointRight(2).longValueExact();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInMinorUnits)
                    .setCurrency(currencyCode.toLowerCase())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(
                                            PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.ALWAYS)
                                    .build())
                    .putMetadata("internalPaymentId", String.valueOf(payment.getId()))
                    .putMetadata("accountId", String.valueOf(account.getId()))
                    .putMetadata("clientId", String.valueOf(account.getClientId()))
                    .putMetadata("topUp", "true")
                    .setDescription("CashTactics account top-up")
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            payment.setStripePaymentIntentId(intent.getId());
            payment = paymentRepository.save(payment);

            String clientSecret = intent.getClientSecret();
            if (clientSecret == null || clientSecret.isBlank()) {
                throw new PaymentFailedException("Stripe did not return a client secret");
            }

            return new TopUpIntentResponse(
                    clientSecret,
                    payment.getId(),
                    intent.getId(),
                    currencyCode.toUpperCase(),
                    amount);

        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Amount must have at most 2 decimal places");
        } catch (StripeException e) {
            log.error("Stripe PaymentIntent (top-up) failed: {}", e.getMessage());
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new PaymentFailedException("Could not start payment: " + e.getMessage(), e);
        }
    }

    //---- Create a Stripe PaymentIntent and persist locally-----
    public PaymentDTO createPayment(CreatePaymentRequest req) {
        Payment payment = PaymentMapper.toEntity(req);

        try {
            //Bc Stripe expects amount in cents
            long amountInCents = req.getAmount().movePointRight(2).longValueExact();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(req.getCurrencyCode().toLowerCase())
                    .setPaymentMethod(req.getPaymentMethodId())
                    .setConfirm(true)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(
                                            PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            payment.setStripePaymentIntentId(intent.getId());

            //Map Stripe status to my enums
            if ("succeeded".equals(intent.getStatus())) {
                payment.setStatus(PaymentStatus.COMPLETED);
            } else {
                payment.setStatus(PaymentStatus.PENDING);
            }

        } catch (StripeException e) {
            log.error("Stripe PaymentIntent creation failed: {}", e.getMessage());
            payment.setStatus(PaymentStatus.FAILED);
            payment = paymentRepository.save(payment);
            throw new PaymentFailedException("Payment failed: " + e.getMessage(), e);
        }

        payment = paymentRepository.save(payment);
        return PaymentMapper.toDTO(payment);
    }

    //---Get payment by id----
    public PaymentDTO getById(Long id) {
        Payment payment = findPaymentOrThrow(id);
        return PaymentMapper.toDTO(payment);
    }

    //---Get ALL for a client---
    public List<PaymentDTO> getPaymentsByClient(Long clientId) {
        return paymentRepository.findByClientIdOrderByCreatedAtDesc(clientId)
                .stream()
                .map(PaymentMapper::toDTO)
                .toList();
    }

    //----Refund a completed payment----
    public PaymentDTO refundPayment(Long id) {
        Payment payment = findPaymentOrThrow(id);

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new PaymentFailedException("Only completed payments can be refunded. Current status: " + payment.getStatus());
        }

        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(payment.getStripePaymentIntentId())
                    .build();

            Refund.create(params);

            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setUpdatedAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);
        } catch (StripeException e) {
            log.error("Strip refund failed for payment {}: {}", id, e.getMessage());
            throw new PaymentFailedException("Refund failed: " + e.getMessage(), e);
        }

        return PaymentMapper.toDTO(payment);
    }

    //----Handle Stripe Webhook events----
    public void handleWebhookEvent(String eventType, String paymentIntentId) {
        Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElse(null);

        if (payment == null) {
            log.warn("Webhook: no payment found for intent {}", paymentIntentId);
            return;
        }

        switch (eventType) {
            case "payment_intent.succeeded" -> handlePaymentIntentSucceeded(payment);
            case "payment_intent.payment_failed" -> {
                if (payment.getStatus() != PaymentStatus.COMPLETED) {
                    payment.setStatus(PaymentStatus.FAILED);
                }
            }
            case "charge.refunded" -> payment.setStatus(PaymentStatus.REFUNDED);
            default -> {
                log.info("Webhook: unhandled event type {}", eventType);
                return;
            }
        }

        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        log.info("Webhook: payment {} status {}", paymentIntentId, payment.getStatus());
    }

    private void handlePaymentIntentSucceeded(Payment payment) {
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            log.info("Webhook: idempotent skip for intent {}", payment.getStripePaymentIntentId());
            return;
        }
        applyCreditViaAccountService(payment);
        payment.setStatus(PaymentStatus.COMPLETED);
    }

    /**
     * Credits the ledger in account-service (which also writes a DEPOSIT in transaction-service).
     */
    private void applyCreditViaAccountService(Payment payment) {
        String url = accountServiceBaseUrl.replaceAll("/$", "") + "/api/internal/stripe-top-up/apply";

        Map<String, Object> body = new HashMap<>();
        body.put("accountId", payment.getAccountId());
        body.put("amount", payment.getAmount());
        body.put("currencyCode", payment.getCurrency().getCode());
        body.put("stripePaymentIntentId", payment.getStripePaymentIntentId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Api-Secret", internalApiSecret);

        try {
            restTemplate.postForObject(url, new HttpEntity<>(body, headers), Void.class);
        } catch (Exception e) {
            log.error("Failed to apply Stripe top-up credit for intent {}: {}", payment.getStripePaymentIntentId(), e.getMessage());
            throw new RuntimeException("Settlement failed: " + e.getMessage(), e);
        }
    }

    //Helper
    private Payment findPaymentOrThrow(Long id) {
        return paymentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }
}
