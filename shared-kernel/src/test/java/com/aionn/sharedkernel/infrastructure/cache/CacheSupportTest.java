package com.aionn.sharedkernel.infrastructure.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aionn.sharedkernel.infrastructure.cache.core.CaffeineRedisTwoTierCache;
import com.aionn.sharedkernel.infrastructure.cache.core.TwoTierCacheProperties;
import com.aionn.sharedkernel.infrastructure.cache.invalidation.CacheInvalidationMessage;
import com.aionn.sharedkernel.infrastructure.cache.invalidation.CacheInvalidationPublisher;
import com.aionn.sharedkernel.infrastructure.cache.invalidation.RedisCacheInvalidationPublisher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class CacheSupportTest {

    @Test
    void twoTierCachePropertiesValidateInputs() {
        assertThrows(IllegalArgumentException.class, () -> new TwoTierCacheProperties(" ", Duration.ofSeconds(1), 1, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new TwoTierCacheProperties("ns", Duration.ZERO, 1, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new TwoTierCacheProperties("ns", Duration.ofSeconds(1), 0, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new TwoTierCacheProperties("ns", Duration.ofSeconds(1), 1, Duration.ZERO));
    }

    @Test
    void redisInvalidationPublisherSerializesAndWrapsFailures() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();
        RedisCacheInvalidationPublisher publisher = new RedisCacheInvalidationPublisher(redisTemplate, objectMapper);
        CacheInvalidationMessage message = new CacheInvalidationMessage("catalog", "sku-1", "node-a", false);

        publisher.publish(message);
        verify(redisTemplate).convertAndSend(anyString(), anyString());

        ObjectMapper failingMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws com.fasterxml.jackson.core.JsonProcessingException {
                throw new JsonMappingException(null, "boom");
            }
        };
        RedisCacheInvalidationPublisher failingPublisher = new RedisCacheInvalidationPublisher(redisTemplate, failingMapper);
        assertThrows(IllegalStateException.class, () -> failingPublisher.publish(message));
    }

    @Test
    void caffeineRedisTwoTierCacheHandlesHappyPathAndFallbackPaths() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ObjectMapper objectMapper = new ObjectMapper();
        CacheInvalidationPublisher invalidationPublisher = mock(CacheInvalidationPublisher.class);
        TwoTierCacheProperties properties = new TwoTierCacheProperties("catalog", Duration.ofMinutes(5), 100, Duration.ofMinutes(10));

        CaffeineRedisTwoTierCache<Map<String, String>> cache = new CaffeineRedisTwoTierCache<>(
                properties,
                redisTemplate,
                objectMapper,
                new TypeReference<>() {
                },
                invalidationPublisher,
                "node-a");

        Map<String, String> value = Map.of("sku", "sku-1");
        cache.put("k1", value);
        assertEquals("catalog", cache.namespace());
        assertEquals(value, cache.get("k1").orElseThrow());

        when(valueOperations.get("cache:catalog:k2")).thenReturn("{\"sku\":\"sku-2\"}");
        assertEquals("sku-2", cache.get("k2").orElseThrow().get("sku"));

        Map<String, String> loaded = cache.getOrLoad("k3", () -> Map.of("sku", "sku-3"));
        assertEquals("sku-3", loaded.get("sku"));

        cache.evict("k1");
        verify(invalidationPublisher).publish(new CacheInvalidationMessage("catalog", "k1", "node-a", false));

        when(valueOperations.get("cache:catalog:k4")).thenThrow(new RuntimeException("redis down"));
        assertFalse(cache.get("k4").isPresent());

        when(redisTemplate.delete("cache:catalog:k5")).thenThrow(new RuntimeException("redis down"));
        cache.evict("k5");

        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(null);
        cache.evictAll();
        verify(invalidationPublisher).publish(new CacheInvalidationMessage("catalog", null, "node-a", true));

        cache.invalidateLocal("k2");
        cache.invalidateAllLocal();
        assertNotNull(cache);
    }
}
