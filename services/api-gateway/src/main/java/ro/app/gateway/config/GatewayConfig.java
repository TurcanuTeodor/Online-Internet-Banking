package ro.app.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ro.app.gateway.filter.JwtAuthFilter;

@Configuration
public class GatewayConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final String authServiceUrl;
    private final String clientServiceUrl;
    private final String accountServiceUrl;
    private final String transactionServiceUrl;
    private final String paymentServiceUrl;

    public GatewayConfig(
            JwtAuthFilter jwtAuthFilter,
            @Value("${services.auth.url:http://localhost:8081}") String authServiceUrl,
            @Value("${services.client.url:http://localhost:8082}") String clientServiceUrl,
            @Value("${services.account.url:http://localhost:8083}") String accountServiceUrl,
            @Value("${services.transaction.url:http://localhost:8084}") String transactionServiceUrl,
            @Value("${services.payment.url:http://localhost:8085}") String paymentServiceUrl) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authServiceUrl = authServiceUrl;
        this.clientServiceUrl = clientServiceUrl;
        this.accountServiceUrl = accountServiceUrl;
        this.transactionServiceUrl = transactionServiceUrl;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()

                // AUTH — public (login, register, 2fa) — fara JWT
                .route("auth-service", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb
                                        .setName("authCB")
                                        .setFallbackUri("forward:/fallback/auth"))
                        )
                        .uri(authServiceUrl))

                // Client sign-up (no JWT) — must be before /api/clients/** catch-all
                .route("client-sign-up", r -> r
                        .path("/api/clients/sign-up")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb
                                        .setName("clientSignUpCB")
                                        .setFallbackUri("forward:/fallback/service"))
                        )
                        .uri(clientServiceUrl))

                // CLIENTS — protejat cu JWT
                .route("client-service", r -> r
                        .path("/api/clients/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(new JwtAuthFilter.Config()))
                                .circuitBreaker(cb -> cb
                                        .setName("clientCB")
                                        .setFallbackUri("forward:/fallback/service"))
                        )
                        .uri(clientServiceUrl))

                // GDPR (client-service) — protejat cu JWT
                .route("client-gdpr", r -> r
                        .path("/api/gdpr/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(new JwtAuthFilter.Config()))
                                .circuitBreaker(cb -> cb
                                        .setName("clientGdprCB")
                                        .setFallbackUri("forward:/fallback/service"))
                        )
                        .uri(clientServiceUrl))

                // ACCOUNTS — protejat cu JWT
                .route("account-service", r -> r
                        .path("/api/accounts/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(new JwtAuthFilter.Config()))
                                .circuitBreaker(cb -> cb
                                        .setName("accountCB")
                                        .setFallbackUri("forward:/fallback/service"))
                        )
                        .uri(accountServiceUrl))

                // TRANSACTIONS — protejat cu JWT
                .route("transaction-service", r -> r
                        .path("/api/transactions/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(new JwtAuthFilter.Config()))
                                .circuitBreaker(cb -> cb
                                        .setName("transactionCB")
                                        .setFallbackUri("forward:/fallback/service"))
                        )
                        .uri(transactionServiceUrl))

                // Stripe webhook — no JWT (Stripe-Signature only); must be registered before /api/payments/**
                .route("payment-webhook", r -> r
                        .path("/api/payments/webhook")
                        .uri(paymentServiceUrl))

                // PAYMENTS — protejat cu JWT
                .route("payment-service", r -> r
                        .path("/api/payments/**", "/api/payment-methods/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(new JwtAuthFilter.Config()))
                                .circuitBreaker(cb -> cb
                                        .setName("paymentCB")
                                        .setFallbackUri("forward:/fallback/service"))
                        )
                        .uri(paymentServiceUrl))

                .build();
    }
}
