package ro.app.payment.security.jwt;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import ro.app.payment.config.PaymentProperties;

@Service
public class JwtService { // validate-only — no token generation

    private final SecretKey key;
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    public JwtService(PaymentProperties paymentProperties) {
        this.key = Keys.hmacShaKeyFor(paymentProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public Claims parseClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("JWT token is null or blank");
        }

        return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }
}
