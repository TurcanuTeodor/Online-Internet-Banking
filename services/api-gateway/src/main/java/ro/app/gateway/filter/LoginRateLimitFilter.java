package ro.app.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

/**
 * POST /api/auth/login — max {@value #MAX_REQUESTS} requests per {@value #WINDOW_SECONDS} seconds per client IP.
 */
@Component
public class LoginRateLimitFilter implements GlobalFilter, Ordered {

    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_SECONDS = 60;

    private final Map<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (!"POST".equals(request.getMethod().name())) {
            return chain.filter(exchange);
        }
        String path = request.getURI().getPath();
        if (!"/api/auth/login".equals(path)) {
            return chain.filter(exchange);
        }

        String ip = clientIp(request);
        long now = System.currentTimeMillis();
        Deque<Long> times = buckets.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());

        synchronized (times) {
            long cutoff = now - TimeUnit.SECONDS.toMillis(WINDOW_SECONDS);
            while (!times.isEmpty() && times.peekFirst() < cutoff) {
                times.pollFirst();
            }
            if (times.size() >= MAX_REQUESTS) {
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                return exchange.getResponse().setComplete();
            }
            times.addLast(now);
        }

        return chain.filter(exchange);
    }

    private static String clientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    @Override
    public int getOrder() {
        return -10;
    }
}
