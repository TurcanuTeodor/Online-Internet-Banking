package ro.app.auth.service;

import java.util.Map;

import org.springframework.stereotype.Service;

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

    public RefreshTokenResponse refreshToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(refreshTokenValue);
        User user = refreshToken.getUser();

        Long clientId = user.getClientId();
        boolean twoFaVerified = user.isTwoFactorEnabled();

        String newToken = jwtService.generateToken(
                user.getUsernameOrEmail(),
                Map.of(
                        "role", user.getRole().name(),
                        "clientId", clientId,
                        "2fa", "ok",
                        "2fa_verified", twoFaVerified
                ));

        refreshTokenService.revokeRefreshToken(refreshTokenValue);
        String newRefreshTokenValue = refreshTokenService.createRefreshToken(user);

        return new RefreshTokenResponse(newToken, newRefreshTokenValue);
    }
}
