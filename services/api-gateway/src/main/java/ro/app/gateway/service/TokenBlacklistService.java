package ro.app.gateway.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;
import ro.app.gateway.security.JwtService;

@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String BLACKLIST_PREFIX = "gateway:blacklist:token:";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final JwtService jwtService;

    public TokenBlacklistService(ReactiveStringRedisTemplate redisTemplate, JwtService jwtService) {
        this.redisTemplate = redisTemplate;
        this.jwtService = jwtService;
    }

    public Mono<Boolean> isBlacklisted(String token) {
        return redisTemplate.hasKey(keyFor(token));
    }

    public Mono<Void> blacklist(String token) {
        return Mono.fromCallable(() -> jwtService.parseClaims(token))
                .flatMap(claims -> {
                    Duration ttl = remainingTtl(claims);
                    if (ttl.isZero() || ttl.isNegative()) {
                        return Mono.empty();
                    }
                    return redisTemplate.opsForValue()
                            .set(keyFor(token), "1", ttl)
                            .then();
                })
                .onErrorResume(ex -> {
                    log.debug("Skipping blacklist for token: {}", ex.getMessage());
                    return Mono.empty();
                });
    }

    private Duration remainingTtl(Claims claims) {
        if (claims.getExpiration() == null) {
            return Duration.ZERO;
        }
        Instant now = Instant.now();
        Instant exp = claims.getExpiration().toInstant();
        return Duration.between(now, exp);
    }

    private String keyFor(String token) {
        return BLACKLIST_PREFIX + sha256(token);
    }

    private String sha256(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}