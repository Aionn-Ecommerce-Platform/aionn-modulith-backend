package com.aionn.sharedkernel.infrastructure.cache.invalidation;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;

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
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to serialize cache invalidation for " + message.namespace(), ex);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to publish cache invalidation for " + message.namespace(), ex);
        }
    }
}
