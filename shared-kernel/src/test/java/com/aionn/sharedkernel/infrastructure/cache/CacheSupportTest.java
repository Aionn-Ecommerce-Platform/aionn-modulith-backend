package com.aionn.sharedkernel.infrastructure.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
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
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class CacheSupportTest {

    @Test
    void twoTierCachePropertiesValidateInputs() {
        Duration oneSecond = Duration.ofSeconds(1);
        assertThrows(IllegalArgumentException.class, () -> new TwoTierCacheProperties(null, oneSecond, 1, oneSecond));
        assertThrows(IllegalArgumentException.class, () -> new TwoTierCacheProperties("ns", null, 1, oneSecond));
        assertThrows(IllegalArgumentException.class, () -> new TwoTierCacheProperties("ns", oneSecond, 1, null));
        assertThrows(IllegalArgumentException.class, () -> new TwoTierCacheProperties(" ", oneSecond, 1, oneSecond));
        assertThrows(IllegalArgumentException.class,
                () -> new TwoTierCacheProperties("ns", Duration.ZERO, 1, oneSecond));
        assertThrows(IllegalArgumentException.class, () -> new TwoTierCacheProperties("ns", oneSecond, 0, oneSecond));
        assertThrows(IllegalArgumentException.class,
                () -> new TwoTierCacheProperties("ns", oneSecond, 1, Duration.ZERO));
    }

    @Test
    void redisInvalidationPublisherSerializesAndWrapsFailures() {
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
        RedisCacheInvalidationPublisher failingPublisher = new RedisCacheInvalidationPublisher(redisTemplate,
                failingMapper);
        assertThrows(IllegalStateException.class, () -> failingPublisher.publish(message));
    }

    @Test
    void caffeineRedisTwoTierCacheReadsFromL1L2AndLoader() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ObjectMapper objectMapper = new ObjectMapper();
        CacheInvalidationPublisher invalidationPublisher = mock(CacheInvalidationPublisher.class);
        TwoTierCacheProperties properties = new TwoTierCacheProperties("catalog", Duration.ofMinutes(5), 100,
                Duration.ofMinutes(10));

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
    }

    @Test
    void caffeineRedisTwoTierCacheHandlesInvalidationAndRedisFallbacks() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ObjectMapper objectMapper = new ObjectMapper();
        CacheInvalidationPublisher invalidationPublisher = mock(CacheInvalidationPublisher.class);
        TwoTierCacheProperties properties = new TwoTierCacheProperties("catalog", Duration.ofMinutes(5), 100,
                Duration.ofMinutes(10));

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

        cache.evict("k1");
        verify(invalidationPublisher).publish(new CacheInvalidationMessage("catalog", "k1", "node-a", false));

        when(valueOperations.get("cache:catalog:k4")).thenThrow(new RuntimeException("redis down"));
        assertFalse(cache.get("k4").isPresent());

        when(redisTemplate.delete("cache:catalog:k5")).thenThrow(new RuntimeException("redis down"));
        cache.evict("k5");

        when(redisTemplate.execute(anyRedisCallback())).thenReturn(null);
        cache.evictAll();
        verify(invalidationPublisher).publish(new CacheInvalidationMessage("catalog", null, "node-a", true));

        cache.invalidateLocal("k2");
        cache.invalidateAllLocal();
        assertNotNull(cache);
    }

    @Test
    void putKeepsL1EntryWhenL2SerializationFails() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = valueOps();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ObjectMapper failingMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws com.fasterxml.jackson.core.JsonProcessingException {
                throw new JsonMappingException(null, "boom");
            }
        };
        CacheInvalidationPublisher invalidationPublisher = mock(CacheInvalidationPublisher.class);
        TwoTierCacheProperties properties = new TwoTierCacheProperties(
                "catalog", Duration.ofMinutes(5), 100, Duration.ofMinutes(10));

        CaffeineRedisTwoTierCache<Map<String, String>> cache = new CaffeineRedisTwoTierCache<>(
                properties, redisTemplate, failingMapper, new TypeReference<>() {
                }, invalidationPublisher, "node-a");

        Map<String, String> value = Map.of("sku", "sku-1");
        cache.put("k1", value);

        assertEquals(value, cache.get("k1").orElseThrow());
    }

    @Test
    void getOrLoadReturnsNullWhenLoaderYieldsNull() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = valueOps();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        CaffeineRedisTwoTierCache<Map<String, String>> cache = newCache(redisTemplate);

        Map<String, String> loaded = cache.getOrLoad("missing", () -> null);

        assertNull(loaded);
    }

    @Test
    void evictAllScansAndDeletesMatchingRedisKeys() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = valueOps();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        CacheInvalidationPublisher invalidationPublisher = mock(CacheInvalidationPublisher.class);

        CaffeineRedisTwoTierCache<Map<String, String>> cache = new CaffeineRedisTwoTierCache<>(
                new TwoTierCacheProperties("catalog", Duration.ofMinutes(5), 100, Duration.ofMinutes(10)),
                redisTemplate, new ObjectMapper(), new TypeReference<>() {
                }, invalidationPublisher, "node-a");

        Cursor<byte[]> cursor = byteCursor();
        doReturn(true, true, false).when(cursor).hasNext();
        doReturn("cache:catalog:a".getBytes(), "cache:catalog:b".getBytes()).when(cursor).next();
        RedisKeyCommands keyCommands = mock(RedisKeyCommands.class);
        doReturn(cursor).when(keyCommands).scan(any(ScanOptions.class));
        RedisConnection connection = mock(RedisConnection.class);
        when(connection.keyCommands()).thenReturn(keyCommands);
        when(redisTemplate.execute(anyRedisCallback())).thenAnswer(invocation -> {
            RedisCallback<?> callback = invocation.getArgument(0);
            return callback.doInRedis(connection);
        });

        cache.evictAll();

        verify(keyCommands).del(any(byte[][].class));
        verify(invalidationPublisher).publish(new CacheInvalidationMessage("catalog", null, "node-a", true));
    }

    @Test
    void evictAllStillPublishesWhenScanFails() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = valueOps();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        CacheInvalidationPublisher invalidationPublisher = mock(CacheInvalidationPublisher.class);

        CaffeineRedisTwoTierCache<Map<String, String>> cache = new CaffeineRedisTwoTierCache<>(
                new TwoTierCacheProperties("catalog", Duration.ofMinutes(5), 100, Duration.ofMinutes(10)),
                redisTemplate, new ObjectMapper(), new TypeReference<>() {
                }, invalidationPublisher, "node-a");

        when(redisTemplate.execute(anyRedisCallback())).thenThrow(new RuntimeException("redis down"));

        cache.evictAll();

        verify(invalidationPublisher).publish(new CacheInvalidationMessage("catalog", null, "node-a", true));
    }

    private static CaffeineRedisTwoTierCache<Map<String, String>> newCache(StringRedisTemplate redisTemplate) {
        return new CaffeineRedisTwoTierCache<>(
                new TwoTierCacheProperties("catalog", Duration.ofMinutes(5), 100, Duration.ofMinutes(10)),
                redisTemplate, new ObjectMapper(), new TypeReference<>() {
                }, mock(CacheInvalidationPublisher.class), "node-a");
    }

    private static ValueOperations<String, String> valueOps() {
        return mock(ValueOperations.class);
    }

    private static Cursor<byte[]> byteCursor() {
        return mock(Cursor.class);
    }

    private static RedisCallback<Object> anyRedisCallback() {
        return any(RedisCallback.class);
    }
}
