package ro.app.gateway.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class RedisRateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimitService.class);

    private final ReactiveStringRedisTemplate redisTemplate;

    public RedisRateLimitService(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Boolean> allowRequest(String key, long maxRequests, Duration window) {
        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    if (count == 1L) {
                        return redisTemplate.expire(key, window).thenReturn(true);
                    }
                    return Mono.just(count <= maxRequests);
                })
                .onErrorResume(ex -> {
                    log.warn("Redis rate limit check failed for key {}: {}", key, ex.getMessage());
                    return Mono.just(true);
                });
    }
}