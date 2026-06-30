package com.aionn.sharedkernel.infrastructure.web.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisIdempotencyStore {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<IdempotencyRecord> find(String key) {
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, IdempotencyRecord.class));
        } catch (Exception ex) {
            log.error("Failed to read idempotency record for key {}", key, ex);
            throw new IllegalStateException("Failed to read idempotency record", ex);
        }
    }

    public boolean beginProcessing(String key, String requestHash, Duration ttl) {
        try {
            String payload = objectMapper.writeValueAsString(IdempotencyRecord.processing(requestHash));
            return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, payload, ttl));
        } catch (Exception ex) {
            log.error("Failed to create idempotency processing marker for key {}", key, ex);
            throw new IllegalStateException("Failed to create idempotency processing marker", ex);
        }
    }

    public void saveCompleted(
            String key,
            String requestHash,
            IdempotencyRecord.StoredHttpResponse response,
            Duration ttl) {
        try {
            String payload = objectMapper.writeValueAsString(IdempotencyRecord.completed(requestHash, response));
            stringRedisTemplate.opsForValue().set(key, payload, ttl);
        } catch (Exception ex) {
            log.error("Failed to persist idempotency response for key {}", key, ex);
            throw new IllegalStateException("Failed to persist idempotency response", ex);
        }
    }

    public void delete(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception ex) {
            log.error("Failed to delete idempotency key {}", key, ex);
            throw new IllegalStateException("Failed to delete idempotency key", ex);
        }
    }
}
