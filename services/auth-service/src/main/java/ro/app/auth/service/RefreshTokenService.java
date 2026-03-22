package ro.app.auth.service;

import java.time.LocalDateTime;
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

    public RefreshToken createRefreshToken(User user) {
        String tokenValue = jwtService.generateRefreshToken(user.getUsernameOrEmail());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = now.plusDays(7);

        RefreshToken refreshToken = new RefreshToken(tokenValue, user, expiryDate, now);

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken verifyRefreshToken(String token) {
        Optional<RefreshToken> refreshToken = findByToken(token);

        if (refreshToken.isEmpty()) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        RefreshToken rt = refreshToken.get();

        if (!rt.isValid()) {
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        if (!jwtService.isValid(token)) {
            throw new IllegalArgumentException("Invalid refresh token signature");
        }

        return rt;
    }

    public void revokeRefreshToken(String token) {
        Optional<RefreshToken> refreshToken = findByToken(token);

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
}
