package com.aionn.sharedkernel.infrastructure.cache.invalidation;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
public class RedisCacheInvalidationPublisher implements CacheInvalidationPublisher {

    public static final String CHANNEL = "cache.invalidation";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCacheInvalidationPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(CacheInvalidationMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(CHANNEL, payload);
        } catch (Exception ex) {
            log.warn("Failed to publish cache invalidation for {}: {}", message.namespace(), ex.getMessage());
        }
    }
}
