package ro.app.gateway.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;

import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;

import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;
import ro.app.gateway.security.JwtService;

/**
 * Teste unitare pentru TokenBlacklistService (JUnit 4).
 *
 * Acopera:
 *   1. Blacklist token valid — hash stocat in Redis cu TTL > 0
 *   2. isBlacklisted — verifica cheia hash in Redis
 *   3. Token expirat — nu scrie in Redis (TTL ar fi negativ)
 *
 * Securitate: token-ul este stocat ca SHA-256 hash (nu in clar).
 */
@RunWith(MockitoJUnitRunner.class)
public class TokenBlacklistServiceTest {

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @Mock
    private JwtService jwtService;

    @Test
    public void blacklist_validToken_storesHashedKeyWithPositiveTtl() {
        // Arrange
        String token = "access-token-123";
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        Instant expiration = Instant.now().plusSeconds(90);

        when(jwtService.parseClaims(token)).thenReturn(claims);
        when(claims.getExpiration()).thenReturn(Date.from(expiration));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.set(eq(expectedKey(token)), eq("1"), any(Duration.class)))
                .thenReturn(Mono.just(Boolean.TRUE));

        TokenBlacklistService service = new TokenBlacklistService(redisTemplate, jwtService);

        // Act
        service.blacklist(token).block();

        // Assert — TTL trebuie sa fie pozitiv (token nu e expirat)
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(eq(expectedKey(token)), eq("1"), ttlCaptor.capture());
        assertTrue("TTL must be > 0", ttlCaptor.getValue().compareTo(Duration.ZERO) > 0);
    }

    @Test
    public void isBlacklisted_blacklistedToken_returnsTrue() {
        // Arrange
        String token = "access-token-xyz";
        when(redisTemplate.hasKey(expectedKey(token))).thenReturn(Mono.just(Boolean.TRUE));

        TokenBlacklistService service = new TokenBlacklistService(redisTemplate, jwtService);

        // Act
        boolean result = service.isBlacklisted(token).block();

        // Assert
        assertTrue(result);
        verify(redisTemplate).hasKey(expectedKey(token));
    }

    @Test
    public void blacklist_alreadyExpiredToken_doesNotWriteToRedis() {
        // Arrange — token deja expirat; TTL calculat ar fi negativ
        String token = "expired-token";
        Claims claims = org.mockito.Mockito.mock(Claims.class);

        when(jwtService.parseClaims(token)).thenReturn(claims);
        when(claims.getExpiration()).thenReturn(Date.from(Instant.now().minusSeconds(1)));

        TokenBlacklistService service = new TokenBlacklistService(redisTemplate, jwtService);

        // Act
        service.blacklist(token).block();

        // Assert — nu trebuie sa scrie nimic in Redis
        verify(redisTemplate, org.mockito.Mockito.never()).opsForValue();
        verify(valueOperations, org.mockito.Mockito.never()).set(any(), any(), any(Duration.class));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String expectedKey(String token) {
        return "gateway:blacklist:token:" + sha256(token);
    }

    private static String sha256(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}