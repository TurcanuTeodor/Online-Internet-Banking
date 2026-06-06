package ro.app.gateway.service;

import java.time.Duration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;

import reactor.core.publisher.Mono;

/**
 * Teste unitare pentru RedisRateLimitService (JUnit 4).
 *
 * Acopera:
 *   1. Prima cerere (count=1) — permisa + TTL setat
 *   2. Cerere care depaseste limita (count > limit) — respinsa, TTL nu e setat
 *
 * API-ul serviciului este reactiv (Mono<Boolean>), deci apelam .block()
 * in teste pentru a extrage valoarea sincrona.
 */
@RunWith(MockitoJUnitRunner.class)
public class RedisRateLimitServiceTest {

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @Test
    public void allowRequest_firstRequest_returnsTrueAndSetsTtl() {
        // Arrange
        String key = "gateway:ratelimit:login:ip:127.0.0.1";
        Duration window = Duration.ofSeconds(60);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(key)).thenReturn(Mono.just(1L));
        when(redisTemplate.expire(key, window)).thenReturn(Mono.just(Boolean.TRUE));

        RedisRateLimitService service = new RedisRateLimitService(redisTemplate);

        // Act
        boolean allowed = service.allowRequest(key, 5, window).block();

        // Assert — prima cerere (count=1) este permisa
        assertTrue(allowed);
        verify(valueOperations).increment(key);
        verify(redisTemplate).expire(key, window);
    }

    @Test
    public void allowRequest_limitExceeded_returnsFalseWithoutSettingTtl() {
        // Arrange — count=6 depaseste limita de 5
        String key = "gateway:ratelimit:global:ip:127.0.0.1";
        Duration window = Duration.ofSeconds(60);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(key)).thenReturn(Mono.just(6L));

        RedisRateLimitService service = new RedisRateLimitService(redisTemplate);

        // Act
        boolean allowed = service.allowRequest(key, 5, window).block();

        // Assert — cererea este respinsa; TTL nu trebuie resetat
        assertFalse(allowed);
        verify(valueOperations).increment(key);
        verify(redisTemplate, never()).expire(key, window);
    }

    @Test
    public void allowRequest_atExactLimit_returnsTrueWithoutSettingTtlAgain() {
        // Arrange — count=5, exact la limita (cererea este permisa, TTL nu se reseteaza)
        // Nota: TTL se seteaza DOAR la prima cerere (count==1), nu la fiecare
        String key = "gateway:ratelimit:api:ip:10.0.0.1";
        Duration window = Duration.ofSeconds(60);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(key)).thenReturn(Mono.just(5L));

        RedisRateLimitService service = new RedisRateLimitService(redisTemplate);

        // Act — Boundary: exact la limita
        boolean allowed = service.allowRequest(key, 5, window).block();

        // Assert
        assertTrue(allowed);
        verify(redisTemplate, never()).expire(key, window);
    }
}