package com.aionn.sharedkernel.infrastructure.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.aionn.sharedkernel.infrastructure.cache.core.CacheOrigin;
import com.aionn.sharedkernel.infrastructure.cache.core.TwoTierCache;
import com.aionn.sharedkernel.infrastructure.cache.core.TwoTierCacheProperties;
import com.aionn.sharedkernel.infrastructure.cache.factory.TwoTierCacheFactory;
import com.aionn.sharedkernel.infrastructure.cache.factory.TwoTierCacheRegistry;
import com.aionn.sharedkernel.infrastructure.cache.invalidation.CacheInvalidationPublisher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

class CacheFactorySupportTest {

    @Test
    void cacheOriginUsesConfiguredValueOrGeneratesOne() {
        CacheOrigin configured = new CacheOrigin("node-a");
        CacheOrigin generated = new CacheOrigin(" ");

        assertEquals("node-a", configured.value());
        org.junit.jupiter.api.Assertions.assertNotNull(generated.value());
        org.junit.jupiter.api.Assertions.assertFalse(generated.value().isBlank());
    }

    @Test
    void cacheRegistryRegistersFindsAndRejectsDuplicateNamespaces() {
        TwoTierCacheRegistry registry = new TwoTierCacheRegistry();
        @SuppressWarnings("unchecked")
        TwoTierCache<String, String> cache = mock(TwoTierCache.class);
        @SuppressWarnings("unchecked")
        TwoTierCache<String, String> duplicate = mock(TwoTierCache.class);

        org.mockito.Mockito.when(cache.namespace()).thenReturn("catalog");
        org.mockito.Mockito.when(duplicate.namespace()).thenReturn("catalog");

        registry.register(cache);
        assertSame(cache, registry.find("catalog").orElseThrow());
        assertEquals(1, registry.all().size());
        assertThrows(IllegalStateException.class, () -> registry.register(duplicate));
    }

    @Test
    void cacheFactoryCreatesCacheAndRegistersIt() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();
        CacheInvalidationPublisher invalidationPublisher = mock(CacheInvalidationPublisher.class);
        CacheOrigin cacheOrigin = new CacheOrigin("node-a");
        TwoTierCacheRegistry registry = org.mockito.Mockito.spy(new TwoTierCacheRegistry());
        TwoTierCacheFactory factory = new TwoTierCacheFactory(
                redisTemplate,
                objectMapper,
                invalidationPublisher,
                cacheOrigin,
                registry);

        TwoTierCacheProperties properties =
                new TwoTierCacheProperties("catalog", Duration.ofMinutes(5), 100, Duration.ofMinutes(10));

        TwoTierCache<String, Map<String, String>> cache = factory.create(properties, new TypeReference<>() {
        });

        assertEquals("catalog", cache.namespace());
        verify(registry).register(cache);
        assertSame(cache, registry.find("catalog").orElseThrow());
    }
}
