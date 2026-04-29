package ro.app.account.config.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisPubSubConfig {

    public static final String ACCOUNT_CACHE_INVALIDATION_CHANNEL = "cache:account:invalidate";

    @Bean
    public ChannelTopic accountCacheInvalidationTopic() {
        return new ChannelTopic(ACCOUNT_CACHE_INVALIDATION_CHANNEL);
    }

    @Bean
    public MessageListenerAdapter cacheInvalidationMessageListenerAdapter(
            CacheInvalidationSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "handleMessage");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter cacheInvalidationMessageListenerAdapter,
            ChannelTopic accountCacheInvalidationTopic) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(cacheInvalidationMessageListenerAdapter, accountCacheInvalidationTopic);
        return container;
    }
}