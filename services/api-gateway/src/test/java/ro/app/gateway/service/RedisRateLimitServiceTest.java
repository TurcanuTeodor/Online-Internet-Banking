package ro.app.gateway.service;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class RedisRateLimitServiceTest {

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @Test
    void allowRequestReturnsTrueAndSetsExpiryOnFirstRequest() {
        String key = "gateway:ratelimit:login:ip:127.0.0.1";
        Duration window = Duration.ofSeconds(60);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(key)).thenReturn(Mono.just(1L));
        when(redisTemplate.expire(key, window)).thenReturn(Mono.just(Boolean.TRUE));

        RedisRateLimitService service = new RedisRateLimitService(redisTemplate);

        boolean allowed = service.allowRequest(key, 5, window).block();

        assertTrue(allowed);
        verify(valueOperations).increment(key);
        verify(redisTemplate).expire(key, window);
    }

    @Test
    void allowRequestReturnsFalseWhenLimitIsExceeded() {
        String key = "gateway:ratelimit:global:ip:127.0.0.1";
        Duration window = Duration.ofSeconds(1);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(key)).thenReturn(Mono.just(6L));

        RedisRateLimitService service = new RedisRateLimitService(redisTemplate);

        boolean allowed = service.allowRequest(key, 5, window).block();

        assertFalse(allowed);
        verify(valueOperations).increment(key);
        verify(redisTemplate, never()).expire(eq(key), eq(window));
    }
}