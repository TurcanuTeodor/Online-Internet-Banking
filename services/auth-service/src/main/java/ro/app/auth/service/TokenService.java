package ro.app.auth.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import ro.app.auth.dto.token.RefreshTokenResponse;
import ro.app.auth.model.entity.RefreshToken;
import ro.app.auth.model.entity.User;
import ro.app.auth.repository.UserRepository;
import ro.app.auth.security.jwt.JwtService;

@Service
public class TokenService {

    private final UserRepository userRepo;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public TokenService(UserRepository userRepo, JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.userRepo = userRepo;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    public RefreshTokenResponse refreshToken(String refreshTokenValue, String previousAccessToken) {
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(refreshTokenValue);
        User user = refreshToken.getUser();

        Long clientId = user.getClientId();
        boolean twoFaVerified = user.isTwoFactorEnabled();

        String ek = null;
        if (previousAccessToken != null && !previousAccessToken.isBlank()) {
            try {
                Claims old = jwtService.parseClaimsAllowExpired(previousAccessToken);
                if (user.getUsernameOrEmail().equals(old.getSubject())) {
                    Object ekClaim = old.get("ek");
                    if (ekClaim != null) {
                        ek = ekClaim.toString();
                    }
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                // ignore invalid signature / malformed token
            }
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("clientId", clientId);
        claims.put("2fa", "ok");
        claims.put("2fa_verified", twoFaVerified);
        if (ek != null && !ek.isBlank()) {
            claims.put("ek", ek);
        }

        String newToken = jwtService.generateToken(user.getUsernameOrEmail(), claims);

        refreshTokenService.revokeRefreshToken(refreshTokenValue);
        String newRefreshTokenValue = refreshTokenService.createRefreshToken(user);

        return new RefreshTokenResponse(newToken, newRefreshTokenValue);
    }
}
