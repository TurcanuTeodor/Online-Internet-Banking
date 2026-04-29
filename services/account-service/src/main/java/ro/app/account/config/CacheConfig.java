package ro.app.account.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CaffeineCacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "exchangeRates",
                "accountsByClient",
                "balance",
                "accountDetails"
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofSeconds(5))
                .maximumSize(5000)
                .recordStats());
        return cacheManager;
    }

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        // Key serializer: String, Value serializer: JSON for easy debugging
        StringRedisSerializer keySer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valSer = new GenericJackson2JsonRedisSerializer();

        RedisSerializationContext.SerializationPair<Object> valuePair =
                RedisSerializationContext.SerializationPair.fromSerializer(valSer);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySer))
                .serializeValuesWith(valuePair)
                .entryTtl(Duration.ofSeconds(30));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("exchangeRates", defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigs.put("accountsByClient", defaultConfig.entryTtl(Duration.ofSeconds(60)));
        cacheConfigs.put("balance", defaultConfig.entryTtl(Duration.ofSeconds(30)));
        cacheConfigs.put("accountDetails", defaultConfig.entryTtl(Duration.ofSeconds(30)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

        @Bean
        @Primary
    public CacheManager cacheManager(CaffeineCacheManager caffeine, RedisCacheManager redis) {
        // CompositeCacheManager consults caches in order: first Caffeine, then Redis
        CompositeCacheManager mgr = new CompositeCacheManager(caffeine, redis);
        mgr.setFallbackToNoOpCache(false);
        return mgr;
    }
}
