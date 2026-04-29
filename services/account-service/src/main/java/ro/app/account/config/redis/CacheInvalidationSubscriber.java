package ro.app.account.config.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class CacheInvalidationSubscriber {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationSubscriber.class);

    private final CaffeineCacheManager caffeineCacheManager;
    private final ObjectMapper objectMapper;

    public CacheInvalidationSubscriber(CaffeineCacheManager caffeineCacheManager, ObjectMapper objectMapper) {
        this.caffeineCacheManager = caffeineCacheManager;
        this.objectMapper = objectMapper;
    }

    public void handleMessage(String message) {
        try {
            CacheInvalidationMessage event = objectMapper.readValue(message, CacheInvalidationMessage.class);

            Cache cache = caffeineCacheManager.getCache(event.cacheName());
            if (cache != null) {
                cache.evict(event.key());
                log.info("Invalidated local Caffeine cache: cacheName={}, key={}, reason={}",
                        event.cacheName(), event.key(), event.reason());
            }
        } catch (Exception e) {
            log.warn("Failed to process cache invalidation message: {}", e.getMessage());
        }
    }
}