package ro.app.auth.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Custom Health Indicator for Redis cache (JWT token blacklist).
 * Checks Redis connectivity and response time.
 * 
 * Endpoint: GET /actuator/health (shows as "redis" component)
 * Returns: UP if Redis is reachable and responsive, DOWN if unreachable
 * 
 * Critical for: JWT blacklist operations during logout
 */
@Component
public class RedisHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(RedisHealthIndicator.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisHealthIndicator(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();
            RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                return Health.down()
                        .withDetail("error", "Redis connection factory is not configured")
                        .build();
            }
            
            // Perform PING test (lightweight, no-op Redis operation)
            String pong = connectionFactory
                    .getConnection()
                    .ping();
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            if ("PONG".equals(pong) || pong != null) {
                log.debug("Redis health check passed in {}ms", responseTime);
                
                return Health.up()
                        .withDetail("status", "PONG")
                        .withDetail("response_time_ms", responseTime)
                        .build();
            } else {
                log.warn("Redis PING returned unexpected response: {}", pong);
                
                return Health.down()
                        .withDetail("error", "Unexpected PING response: " + pong)
                        .build();
            }
            
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("exception_type", e.getClass().getSimpleName())
                    .withException(e)
                    .build();
        }
    }
}
