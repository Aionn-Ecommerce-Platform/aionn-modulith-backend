package com.aionn.platform.infrastructure.cache;

import org.springframework.data.redis.listener.ChannelTopic;

import com.aionn.platform.infrastructure.cache.core.CacheOrigin;
import com.aionn.platform.infrastructure.cache.invalidation.CacheInvalidationListener;
import com.aionn.platform.infrastructure.cache.invalidation.CacheInvalidationPublisher;
import com.aionn.platform.infrastructure.cache.factory.TwoTierCacheRegistry;
import com.aionn.platform.infrastructure.cache.invalidation.RedisCacheInvalidationPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;

@Configuration
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnProperty(prefix = "aionn.shared.cache.two-tier", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TwoTierCacheSettings.class)
public class TwoTierCacheConfig {

    @Bean
    public TwoTierCacheRegistry twoTierCacheRegistry() {
        return new TwoTierCacheRegistry();
    }

    @Bean
    public CacheOrigin cacheOrigin(TwoTierCacheSettings settings) {
        return new CacheOrigin(settings.origin());
    }

    @Bean
    public CacheInvalidationPublisher cacheInvalidationPublisher(
            StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        return new RedisCacheInvalidationPublisher(redisTemplate, objectMapper);
    }

    @Bean
    public CacheInvalidationListener cacheInvalidationListener(
            ObjectMapper objectMapper,
            TwoTierCacheRegistry registry,
            CacheOrigin cacheOrigin) {
        return new CacheInvalidationListener(objectMapper, registry, cacheOrigin.value());
    }

    @Bean
    public RedisMessageListenerContainer cacheInvalidationListenerContainer(
            RedisConnectionFactory connectionFactory,
            CacheInvalidationListener listener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        Topic topic = new ChannelTopic(
                RedisCacheInvalidationPublisher.CHANNEL);
        container.addMessageListener(listener, topic);
        return container;
    }
}
