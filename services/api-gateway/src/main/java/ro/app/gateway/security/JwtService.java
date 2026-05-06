package ro.app.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import ro.app.gateway.config.GatewayProperties;

@Service
public class JwtService {
    private final SecretKey secretKey;
    private final String issuer;

    public JwtService(GatewayProperties gatewayProperties) {
        this.secretKey = Keys.hmacShaKeyFor(gatewayProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        this.issuer = gatewayProperties.getJwt().getIssuer();
    }

    public boolean isValid(String token){
        try{
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;

        }catch(JwtException e) {
            return false;
        }
    }

    public Claims parseClaims(String token){
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
