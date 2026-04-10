package ro.app.auth.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ro.app.auth.internal.InternalApiHeaders;

@Component
public class InternalApiAuthFilter extends OncePerRequestFilter {

    private final byte[] expectedSecret;

    public InternalApiAuthFilter(@Value("${app.internal.api-secret}") String secret) {
        this.expectedSecret = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // Apply only on internal endpoints to avoid impacting regular JWT flows.
        if (!request.getRequestURI().startsWith("/api/internal/")) {
            chain.doFilter(request, response);
            return;
        }

        String provided = request.getHeader(InternalApiHeaders.SECRET);
        if (provided == null
                || !MessageDigest.isEqual(expectedSecret, provided.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return;
        }

        var auth = new PreAuthenticatedAuthenticationToken(
                "internal-service",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_INTERNAL")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(request, response);
    }
}