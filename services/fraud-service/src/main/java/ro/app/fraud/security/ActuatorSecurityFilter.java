package ro.app.fraud.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import ro.app.fraud.config.FraudProperties;

/**
 * Security filter for Spring Boot Actuator endpoints (/actuator/*).
 * Requires X-Internal-Api-Secret header for access to sensitive endpoints.
 * 
 * Public endpoints (health, info): Allow without auth
 * Protected endpoints (metrics, fraud-model, environment, etc.): Require secret header
 * 
 * This prevents exposure of internal metrics, configuration, and ML model parameters.
 */
@Component
public class ActuatorSecurityFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ActuatorSecurityFilter.class);
    private static final String HEADER_SECRET = "X-Internal-Api-Secret";
    
    // These actuator endpoints are public (no auth required)
    private static final String[] PUBLIC_ENDPOINTS = {
            "/actuator",
            "/actuator/health",
            "/actuator/health/live",
            "/actuator/health/ready"
    };
    
    private final byte[] expectedSecret;

    public ActuatorSecurityFilter(FraudProperties fraudProperties) {
        String secret = fraudProperties.getServices().getInternalApiSecret();
        if (secret == null || secret.isBlank()) {
            log.warn("ActuatorSecurityFilter: Internal API secret is not configured");
            this.expectedSecret = new byte[0];
        } else {
            this.expectedSecret = secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        // Only filter /actuator/* requests
        if (!requestPath.startsWith("/actuator/")) {
            chain.doFilter(request, response);
            return;
        }
        
        // Allow public actuator endpoints without authentication
        if (isPublicEndpoint(requestPath)) {
            chain.doFilter(request, response);
            return;
        }
        
        // Protected endpoints require valid secret header
        String providedSecret = request.getHeader(HEADER_SECRET);
        
        if (!isValidSecret(providedSecret)) {
            log.warn("Unauthorized actuator access attempt to {}", requestPath);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write("{\"error\": \"Actuator access requires authentication\"}");
            return;
        }
        
        // Secret is valid - proceed
        chain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String requestPath) {
        for (String publicEndpoint : PUBLIC_ENDPOINTS) {
            if (requestPath.equals(publicEndpoint) || requestPath.startsWith(publicEndpoint + "/")) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidSecret(String providedSecret) {
        if (providedSecret == null || providedSecret.isBlank()) {
            return false;
        }
        
        byte[] providedBytes = providedSecret.getBytes(StandardCharsets.UTF_8);
        return constantTimeEquals(expectedSecret, providedBytes);
    }

    /**
     * Constant-time string comparison to prevent timing attacks
     */
    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Don't filter non-actuator requests
        return !request.getRequestURI().startsWith("/actuator/");
    }
}
