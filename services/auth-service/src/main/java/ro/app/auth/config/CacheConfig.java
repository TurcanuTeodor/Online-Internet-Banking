package ro.app.auth.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

	/**
	 * Cache configuration for auth-service.
	 * - userDetails: caches loaded UserPrincipal to reduce DB hits on every request
	 * TTL 15 min, max 500 entries.
	 */
	@Bean
	public CacheManager cacheManager() {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager(
				"userDetails"
		);
		cacheManager.setCaffeine(Caffeine.newBuilder()
				.expireAfterWrite(Duration.ofMinutes(15))
				.maximumSize(500)
				.recordStats());
		return cacheManager;
	}
}
