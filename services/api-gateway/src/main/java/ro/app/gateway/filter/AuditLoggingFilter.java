package ro.app.gateway.filter;


import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ro.app.gateway.audit.AuditService;
import ro.app.gateway.security.JwtService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class AuditLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger("AUDIT");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JwtService jwtService;
    private final AuditService auditService;

    public AuditLoggingFilter(JwtService jwtService, AuditService auditService) {
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        String ip = getClientIp(request);
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String user = extractUser(request);

        return chain.filter(exchange).doFinally(signalType -> {
           int status = exchange.getResponse().getStatusCode() != null
                   ? exchange.getResponse().getStatusCode().value()
                   :0;

           log.info("[AUDIT] {} | {} | {} {} | status={} | ip={}",
                   timestamp, user, method, path, status, ip);

           if (status == 403 && "GET".equals(method) && "/api/clients/view".equals(path)) {
               JwtRole jwtRole = extractJwtRole(request);
               if ("USER".equals(jwtRole.role())) {
                   auditService.log(
                           "ADMIN_DATA_ACCESS_ATTEMPT",
                           jwtRole.clientId(),
                           jwtRole.role(),
                           null,
                           "Forbidden access to admin client list");
               }
           }
        });
    }

    private JwtRole extractJwtRole(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return new JwtRole(null, null);
        }
        try {
            String token = authHeader.substring(7);
            Claims claims = jwtService.parseClaims(token);
            String role = claims.get("role", String.class);
            Long clientId = claims.get("clientId", Long.class);
            return new JwtRole(clientId, role);
        } catch (Exception e) {
            return new JwtRole(null, null);
        }
    }

    private record JwtRole(Long clientId, String role) {}

    private String extractUser(ServerHttpRequest request){
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            return "anonymous";
        }

        try{
            String token = authHeader.substring(7);
            Claims claims = jwtService.parseClaims(token);
            String subject = claims.getSubject();
            String role = claims.get("role", String.class);
            Long clientId = claims.get("clientId", Long.class);
            return subject + " (role=" + role + ", clientId=" + clientId + ")";
        }catch(Exception e){
            return "invalid-token";
        }
    }

    private String getClientIp(ServerHttpRequest request){
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if(forwarded != null && !forwarded.isBlank()){
            return forwarded.split(",")[0].trim();
        }
        if(request.getRemoteAddress() != null){
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    @Override
    public int getOrder() {
        // Rulează după RateLimitFilter (-2) și după JwtAuthFilter
        // dar înainte de routing (-1) ca să logheze tot
        return -1;
    }
}
