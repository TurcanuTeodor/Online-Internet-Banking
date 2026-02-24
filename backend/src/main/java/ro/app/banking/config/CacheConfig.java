package ro.app.banking.config;

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
	 * Configurare cache pentru rate de schimb valutar.
	 * Cache-ul expire automat după 24 ore (86400 secunde).
	 */
	@Bean
	public CacheManager cacheManager() {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager(
				"exchangeRates",
				"accountsByClient",
				"balance"
		);
		cacheManager.setCaffeine(Caffeine.newBuilder()
				.expireAfterWrite(Duration.ofHours(24)) // TTL de 24 ore
				.maximumSize(1000) // maxim 1000 perechi de conversii
				.recordStats()); // pentru monitoring
		return cacheManager;
	}
}
