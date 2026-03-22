package ro.app.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ro.app.gateway.filter.JwtAuthFilter;

@Configuration
public class GatewayConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public GatewayConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
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
                        .uri("http://localhost:8081"))

                // CLIENTS — protejat cu JWT
                .route("client-service", r -> r
                        .path("/api/clients/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(new JwtAuthFilter.Config()))
                                .circuitBreaker(cb -> cb
                                        .setName("clientCB")
                                        .setFallbackUri("forward:/fallback/service"))
                        )
                        .uri("http://localhost:8082"))

                // GDPR (client-service) — protejat cu JWT
                .route("client-gdpr", r -> r
                        .path("/api/gdpr/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(new JwtAuthFilter.Config()))
                                .circuitBreaker(cb -> cb
                                        .setName("clientGdprCB")
                                        .setFallbackUri("forward:/fallback/service"))
                        )
                        .uri("http://localhost:8082"))

                // ACCOUNTS — protejat cu JWT
                .route("account-service", r -> r
                        .path("/api/accounts/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(new JwtAuthFilter.Config()))
                                .circuitBreaker(cb -> cb
                                        .setName("accountCB")
                                        .setFallbackUri("forward:/fallback/service"))
                        )
                        .uri("http://localhost:8083"))

                // TRANSACTIONS — protejat cu JWT
                .route("transaction-service", r -> r
                        .path("/api/transactions/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(new JwtAuthFilter.Config()))
                                .circuitBreaker(cb -> cb
                                        .setName("transactionCB")
                                        .setFallbackUri("forward:/fallback/service"))
                        )
                        .uri("http://localhost:8084"))

                // Stripe webhook — no JWT (Stripe-Signature only); must be registered before /api/payments/**
                .route("payment-webhook", r -> r
                        .path("/api/payments/webhook")
                        .uri("http://localhost:8085"))

                // PAYMENTS — protejat cu JWT
                .route("payment-service", r -> r
                        .path("/api/payments/**", "/api/payment-methods/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(new JwtAuthFilter.Config()))
                                .circuitBreaker(cb -> cb
                                        .setName("paymentCB")
                                        .setFallbackUri("forward:/fallback/service"))
                        )
                        .uri("http://localhost:8085"))

                .build();
    }
}
