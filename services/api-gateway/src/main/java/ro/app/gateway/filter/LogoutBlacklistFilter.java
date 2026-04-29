package ro.app.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ro.app.gateway.service.TokenBlacklistService;

@Component
public class LogoutBlacklistFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LogoutBlacklistFilter.class);

    private final TokenBlacklistService tokenBlacklistService;
    private final ObjectMapper objectMapper;

    public LogoutBlacklistFilter(TokenBlacklistService tokenBlacklistService, ObjectMapper objectMapper) {
        this.tokenBlacklistService = tokenBlacklistService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!HttpMethod.POST.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getURI().getPath();
        if (!"/api/auth/logout".equals(path)) {
            return chain.filter(exchange);
        }

        String headerToken = bearerToken(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        if (headerToken != null) {
            return blacklistAndContinue(exchange, headerToken, chain);
        }

        return DataBufferUtils.join(exchange.getRequest().getBody())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    String bodyToken = extractAccessToken(bytes);
                    ServerWebExchange mutated = exchange.mutate()
                            .request(decorateRequest(exchange, bytes))
                            .build();

                    if (bodyToken == null || bodyToken.isBlank()) {
                        return chain.filter(mutated);
                    }

                    return blacklistAndContinue(mutated, bodyToken, chain);
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    private Mono<Void> blacklistAndContinue(ServerWebExchange exchange, String token, GatewayFilterChain chain) {
        return tokenBlacklistService.blacklist(token)
                .onErrorResume(ex -> {
                    log.warn("Failed to blacklist logout token: {}", ex.getMessage());
                    return Mono.empty();
                })
                .then(chain.filter(exchange));
    }

    private String bearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring(7).trim();
    }

    private String extractAccessToken(byte[] bodyBytes) {
        try {
            JsonNode root = objectMapper.readTree(bodyBytes);
            JsonNode accessToken = root.get("accessToken");
            if (accessToken == null || accessToken.isNull()) {
                return null;
            }
            return accessToken.asText(null);
        } catch (Exception e) {
            log.warn("Failed to parse logout body for access token: {}", e.getMessage());
            return null;
        }
    }

    private ServerHttpRequestDecorator decorateRequest(ServerWebExchange exchange, byte[] bodyBytes) {
        return new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public Flux<DataBuffer> getBody() {
                return Flux.just(new DefaultDataBufferFactory().wrap(bodyBytes));
            }
        };
    }

    @Override
    public int getOrder() {
        return -30;
    }
}