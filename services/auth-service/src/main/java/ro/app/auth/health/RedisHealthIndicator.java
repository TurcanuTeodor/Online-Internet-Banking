package ro.app.auth.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import ro.app.auth.config.AuthProperties;

/**
 * Custom Health Indicator for Redis cache (JWT token blacklist).
 * Checks Redis connectivity and response time with configurable timeout.
 * 
 * Endpoint: GET /actuator/health (shows as "redis" component)
 * Returns: UP if Redis is reachable and responsive within timeout, DOWN if unreachable or timeout
 * 
 * Critical for: JWT blacklist operations during logout + preventing service hangs
 */
@Component
public class RedisHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(RedisHealthIndicator.class);
    private static final long DEFAULT_TIMEOUT_MS = 3000;

    private final RedisTemplate<String, Object> redisTemplate;
    private final long timeoutMs;

    public RedisHealthIndicator(RedisTemplate<String, Object> redisTemplate, AuthProperties authProperties) {
        this.redisTemplate = redisTemplate;
        this.timeoutMs = authProperties.getRedis().getTimeoutMs();
    }

    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();
            RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                log.warn("Redis connection factory is not configured");
                return Health.down()
                        .withDetail("error", "Redis is not available")
                        .build();
            }
            
            // Execute PING with timeout protection via thread interrupt
            final String[] result = new String[1];
            final Exception[] exception = new Exception[1];
            
            Thread pingThread = new Thread(() -> {
                try {
                    result[0] = connectionFactory.getConnection().ping();
                } catch (Exception e) {
                    exception[0] = e;
                }
            });
            
            pingThread.start();
            
            try {
                // Wait for ping with timeout
                pingThread.join(timeoutMs);
                
                if (pingThread.isAlive()) {
                    // Timeout occurred - interrupt thread
                    pingThread.interrupt();
                    log.warn("Redis health check timed out after {}ms", timeoutMs);
                    return Health.down()
                            .withDetail("error", "Redis health check timeout")
                            .build();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Redis health check interrupted");
                return Health.down()
                        .withDetail("error", "Redis health check interrupted")
                        .build();
            }
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (exception[0] != null) {
                log.warn("Redis health check failed: {}", exception[0].getClass().getSimpleName());
                return Health.down()
                        .withDetail("error", "Redis connection failed")
                        .build();
            }
            
            if ("PONG".equals(result[0]) || result[0] != null) {
                log.debug("Redis health check passed in {}ms", responseTime);
                return Health.up()
                        .withDetail("status", "PONG")
                        .withDetail("response_time_ms", responseTime)
                        .build();
            } else {
                log.warn("Redis PING returned unexpected response");
                return Health.down()
                        .withDetail("error", "Redis connection issue")
                        .build();
            }
            
        } catch (Exception e) {
            log.error("Redis health check error: {}", e.getClass().getSimpleName());
            return Health.down()
                    .withDetail("error", "Redis check failed")
                    .build();
        }
    }
}
