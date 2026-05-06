package ro.app.payment.service.payment.creation;

import java.math.BigDecimal;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

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
import ro.app.payment.service.stripe.StripeCustomerService;

/**
 * Creates Stripe PaymentIntents and persists local {@link Payment} rows (top-up and saved-card flows).
 */
@Service
public class PaymentCreationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentCreationService.class);

    private static final Set<String> TOP_UP_STRIPE_CURRENCIES = Set.of("EUR", "RON");

    private final PaymentRepository paymentRepository;
    private final AccountRestClient accountRestClient;
    private final OwnershipChecker ownershipChecker;
    private final StripeCustomerService stripeCustomerService;

    public PaymentCreationService(
            PaymentRepository paymentRepository,
            AccountRestClient accountRestClient,
            OwnershipChecker ownershipChecker,
            StripeCustomerService stripeCustomerService) {
        this.paymentRepository = paymentRepository;
        this.accountRestClient = accountRestClient;
        this.ownershipChecker = ownershipChecker;
        this.stripeCustomerService = stripeCustomerService;
    }

    /**
     * Card top-up: local {@link Payment} row + Stripe PaymentIntent (unconfirmed).
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
            String customerId = stripeCustomerService.getOrCreateCustomerId(account.getClientId());

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInMinorUnits)
                    .setCurrency(currencyCode.toLowerCase())
                    .setCustomer(customerId)
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

    /**
     * Legacy/saved-card flow: confirm PaymentIntent with existing Stripe PM id.
     */
    public PaymentDTO createPayment(CreatePaymentRequest req) {
        Payment payment = PaymentMapper.toEntity(req);

        try {
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
}
