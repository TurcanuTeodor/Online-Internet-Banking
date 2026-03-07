package ro.app.gateway.filter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final RateLimiter rateLimiter;

    public RateLimitFilter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(50) // Maximum 50 requests per period
                .limitRefreshPeriod(Duration.ofSeconds(1)) // Refresh period of 1 second
                .timeoutDuration(Duration.ZERO) // Timeout duration for acquiring permission
                .build();
        this.rateLimiter = RateLimiter.of("gateway-rate-limiter", config);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if(rateLimiter.acquirePermission()){
            return chain.filter(exchange);
        }

        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -2; // Ensure this filter runs before the default rate limiter filter
    }
}
