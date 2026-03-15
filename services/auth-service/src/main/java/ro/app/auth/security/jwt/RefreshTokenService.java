package ro.app.auth.security.jwt;

import ro.app.auth.model.entity.RefreshToken;
import ro.app.auth.model.entity.User;
import ro.app.auth.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;

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
        // Generate JWT-based refresh token
        String tokenValue = jwtService.generateRefreshToken(user.getUsernameOrEmail());
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = now.plusDays(7); // Default 7 days
        
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
        
        // Also verify the JWT signature
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
    
    /**
     * Automatically clean up expired refresh tokens every hour.
     * Scheduled task to prevent accumulation of expired tokens in DB.
     */
    @Scheduled(fixedDelay = 3600000) // Run every 1 hour
    @Transactional
    public void deleteExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        long deleted = refreshTokenRepository.findAll().stream()
                .filter(token -> token.getExpiryDate().isBefore(now))
                .peek(refreshTokenRepository::delete)
                .count();
        if (deleted > 0) {
            log.info("Deleted {} expired refresh tokens", deleted);
        }
    }
}
