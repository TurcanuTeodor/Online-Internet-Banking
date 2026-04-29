package ro.app.gateway.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;

import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;
import ro.app.gateway.security.JwtService;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @Mock
    private JwtService jwtService;

    @Test
    void blacklistStoresHashedTokenWithTtlUntilExpiration() {
        String token = "access-token-123";
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        Instant expiration = Instant.now().plusSeconds(90);

        when(jwtService.parseClaims(token)).thenReturn(claims);
        when(claims.getExpiration()).thenReturn(Date.from(expiration));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.set(eq(expectedKey(token)), eq("1"), any(Duration.class))).thenReturn(Mono.just(Boolean.TRUE));

        TokenBlacklistService service = new TokenBlacklistService(redisTemplate, jwtService);

        service.blacklist(token).block();

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(eq(expectedKey(token)), eq("1"), ttlCaptor.capture());
        assertTrue(ttlCaptor.getValue().compareTo(Duration.ZERO) > 0);
    }

    @Test
    void isBlacklistedChecksHashedKey() {
        String token = "access-token-xyz";
        when(redisTemplate.hasKey(expectedKey(token))).thenReturn(Mono.just(Boolean.TRUE));

        TokenBlacklistService service = new TokenBlacklistService(redisTemplate, jwtService);

        boolean result = service.isBlacklisted(token).block();

        assertTrue(result);
        verify(redisTemplate).hasKey(expectedKey(token));
    }

    @Test
    void blacklistSkipsExpiredTokenWithoutWritingToRedis() {
        String token = "expired-token";
        Claims claims = org.mockito.Mockito.mock(Claims.class);

        when(jwtService.parseClaims(token)).thenReturn(claims);
        when(claims.getExpiration()).thenReturn(Date.from(Instant.now().minusSeconds(1)));

        TokenBlacklistService service = new TokenBlacklistService(redisTemplate, jwtService);

        service.blacklist(token).block();

        verify(redisTemplate, org.mockito.Mockito.never()).opsForValue();
        verify(valueOperations, org.mockito.Mockito.never()).set(any(), any(), any(Duration.class));
    }

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