package ro.app.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.app.auth.model.entity.RefreshToken;
import ro.app.auth.model.entity.User;
import ro.app.auth.repository.RefreshTokenRepository;
import ro.app.auth.security.jwt.JwtService;

@Service
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtService jwtService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    /**
     * Persists SHA-256 hash of refresh JWT; returns the raw token for the client only.
     */
    public String createRefreshToken(User user) {
        String tokenValue = jwtService.generateRefreshToken(user.getUsernameOrEmail());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = now.plusDays(7);

        RefreshToken refreshToken = new RefreshToken(hashToken(tokenValue), user, expiryDate, now);
        refreshTokenRepository.save(refreshToken);
        return tokenValue;
    }

    public RefreshToken verifyRefreshToken(String rawToken) {
        if (!jwtService.isValid(rawToken)) {
            throw new IllegalArgumentException("Invalid refresh token signature");
        }

        String hash = hashToken(rawToken);
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByTokenHash(hash);

        if (refreshToken.isEmpty()) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        RefreshToken rt = refreshToken.get();

        if (!rt.isValid()) {
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        return rt;
    }

    public void revokeRefreshToken(String rawToken) {
        String hash = hashToken(rawToken);
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByTokenHash(hash);

        if (refreshToken.isPresent()) {
            RefreshToken rt = refreshToken.get();
            rt.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(rt);
            log.info("Refresh token revoked for user: {}", rt.getUser().getUsernameOrEmail());
        }
    }

    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.findByUser(user).forEach(token -> {
            token.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(token);
        });
        log.info("All refresh tokens revoked for user: {}", user.getUsernameOrEmail());
    }

    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteAllExpiredBefore(LocalDateTime.now());
        log.info("Expired refresh tokens cleanup completed at");
    }

    private static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
