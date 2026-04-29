package ro.app.account.config.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CacheInvalidationPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ChannelTopic accountCacheInvalidationTopic;

    public CacheInvalidationPublisher(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            ChannelTopic accountCacheInvalidationTopic) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.accountCacheInvalidationTopic = accountCacheInvalidationTopic;
    }

    public void publish(String cacheName, String key, String reason) {
        try {
            CacheInvalidationMessage event = new CacheInvalidationMessage(cacheName, key, reason);
            String payload = objectMapper.writeValueAsString(event);
            stringRedisTemplate.convertAndSend(accountCacheInvalidationTopic.getTopic(), payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish cache invalidation event", e);
        }
    }
}