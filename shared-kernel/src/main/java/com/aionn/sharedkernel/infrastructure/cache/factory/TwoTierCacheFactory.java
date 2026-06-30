package com.aionn.sharedkernel.infrastructure.cache.factory;

import com.aionn.sharedkernel.infrastructure.cache.core.CacheOrigin;
import com.aionn.sharedkernel.infrastructure.cache.core.CaffeineRedisTwoTierCache;
import com.aionn.sharedkernel.infrastructure.cache.core.TwoTierCache;
import com.aionn.sharedkernel.infrastructure.cache.core.TwoTierCacheProperties;
import com.aionn.sharedkernel.infrastructure.cache.invalidation.CacheInvalidationPublisher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class TwoTierCacheFactory {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheInvalidationPublisher invalidationPublisher;
    private final CacheOrigin cacheOrigin;
    private final TwoTierCacheRegistry registry;

    public TwoTierCacheFactory(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            CacheInvalidationPublisher invalidationPublisher,
            CacheOrigin cacheOrigin,
            TwoTierCacheRegistry registry) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.invalidationPublisher = invalidationPublisher;
        this.cacheOrigin = cacheOrigin;
        this.registry = registry;
    }

    public <V> TwoTierCache<String, V> create(TwoTierCacheProperties properties, TypeReference<V> valueType) {
        CaffeineRedisTwoTierCache<V> cache = new CaffeineRedisTwoTierCache<>(
                properties, redisTemplate, objectMapper, valueType, invalidationPublisher, cacheOrigin.value());
        registry.register(cache);
        return cache;
    }
}
