package com.aionn.sharedkernel.infrastructure.cache.core;

import com.aionn.sharedkernel.infrastructure.cache.invalidation.CacheInvalidationMessage;
import com.aionn.sharedkernel.infrastructure.cache.invalidation.CacheInvalidationPublisher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class CaffeineRedisTwoTierCache<V> implements TwoTierCache<String, V> {

    private final TwoTierCacheProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final TypeReference<V> valueType;
    private final CacheInvalidationPublisher invalidationPublisher;
    private final String origin;
    private final Cache<String, Holder<V>> l1;

    public CaffeineRedisTwoTierCache(
            TwoTierCacheProperties properties,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            TypeReference<V> valueType,
            CacheInvalidationPublisher invalidationPublisher,
            String origin) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.valueType = valueType;
        this.invalidationPublisher = invalidationPublisher;
        this.origin = origin;
        this.l1 = Caffeine.newBuilder()
                .expireAfterWrite(properties.l1Ttl().toMillis(), TimeUnit.MILLISECONDS)
                .maximumSize(properties.l1MaxSize())
                .build();
    }

    @Override
    public String namespace() {
        return properties.namespace();
    }

    @Override
    public Optional<V> get(String key) {
        Holder<V> hit = l1.getIfPresent(key);
        if (hit != null) {
            return Optional.ofNullable(hit.value);
        }
        V fromRedis = readFromRedis(key);
        if (fromRedis != null) {
            l1.put(key, new Holder<>(fromRedis));
            return Optional.of(fromRedis);
        }
        return Optional.empty();
    }

    @Override
    public V getOrLoad(String key, Supplier<V> loader) {
        Optional<V> existing = get(key);
        if (existing.isPresent()) {
            return existing.get();
        }
        V loaded = loader.get();
        if (loaded != null) {
            put(key, loaded);
        }
        return loaded;
    }

    @Override
    public void put(String key, V value) {
        l1.put(key, new Holder<>(value));
        try {
            String serialized = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(redisKey(key), serialized,
                    Duration.ofMillis(properties.l2Ttl().toMillis()));
        } catch (Exception ex) {
            log.warn("Two-tier cache L2 write failed for {}/{}: {}",
                    properties.namespace(), key, ex.getMessage());
        }
    }

    @Override
    public void evict(String key) {
        l1.invalidate(key);
        try {
            redisTemplate.delete(redisKey(key));
        } catch (Exception ex) {
            log.warn("Two-tier cache L2 evict failed for {}/{}: {}",
                    properties.namespace(), key, ex.getMessage());
        }
        invalidationPublisher.publish(new CacheInvalidationMessage(
                properties.namespace(), key, origin, false));
    }

    @Override
    public void evictAll() {
        l1.invalidateAll();
        try {
            redisTemplate.delete(redisTemplate.keys(redisKey("*")));
        } catch (Exception ex) {
            log.warn("Two-tier cache L2 evictAll failed for {}: {}",
                    properties.namespace(), ex.getMessage());
        }
        invalidationPublisher.publish(new CacheInvalidationMessage(
                properties.namespace(), null, origin, true));
    }

    @Override
    public void invalidateLocal(String key) {
        l1.invalidate(key);
    }

    @Override
    public void invalidateAllLocal() {
        l1.invalidateAll();
    }

    private V readFromRedis(String key) {
        try {
            String raw = redisTemplate.opsForValue().get(redisKey(key));
            if (raw == null) {
                return null;
            }
            return objectMapper.readValue(raw, valueType);
        } catch (Exception ex) {
            log.warn("Two-tier cache L2 read failed for {}/{}: {}",
                    properties.namespace(), key, ex.getMessage());
            return null;
        }
    }

    private String redisKey(String key) {
        return "cache:" + properties.namespace() + ":" + key;
    }

    private record Holder<V>(V value) {
    }
}
