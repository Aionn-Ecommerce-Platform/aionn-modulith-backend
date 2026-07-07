package com.aionn.sharedkernel.infrastructure.cache;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.aionn.sharedkernel.infrastructure.cache.core.CacheOrigin;
import com.aionn.sharedkernel.infrastructure.cache.factory.TwoTierCacheRegistry;
import com.aionn.sharedkernel.infrastructure.cache.invalidation.CacheInvalidationListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

class TwoTierCacheConfigTest {

    private final TwoTierCacheConfig config = new TwoTierCacheConfig();

    @Test
    void exposesCacheInfrastructureBeans() {
        TwoTierCacheRegistry registry = config.twoTierCacheRegistry();
        CacheOrigin origin = config.cacheOrigin("node-a");
        ObjectMapper objectMapper = new ObjectMapper();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        assertNotNull(registry);
        assertNotNull(origin);
        assertNotNull(config.cacheInvalidationPublisher(redisTemplate, objectMapper));

        CacheInvalidationListener listener = config.cacheInvalidationListener(objectMapper, registry, origin);
        assertNotNull(listener);

        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisMessageListenerContainer container = config.cacheInvalidationListenerContainer(connectionFactory,
                listener);
        assertNotNull(container);
    }
}
