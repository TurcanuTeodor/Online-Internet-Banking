package ro.app.client.security.jwt;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import ro.app.client.config.ClientProperties;

@Service
public class JwtService { //only validates

    private final SecretKey key;
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    public JwtService(ClientProperties clientProperties) {
        this.key = Keys.hmacShaKeyFor(clientProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
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
