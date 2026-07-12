package com.aionn.sharedkernel.infrastructure.web.idempotency;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisIdempotencyStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisIdempotencyStore store;

    @BeforeEach
    void setUp() {
        store = new RedisIdempotencyStore(redisTemplate, new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void findReturnsEmptyForBlankStoredValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("k")).thenReturn("   ");

        assertTrue(store.find("k").isEmpty());
    }

    @Test
    void findWrapsDeserializationFailure() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("k")).thenReturn("not-json");

        assertThrows(IllegalStateException.class, () -> store.find("k"));
    }

    @Test
    void saveCompletedWrapsRedisFailure() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("redis down"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        IdempotencyRecord.StoredHttpResponse response = new IdempotencyRecord.StoredHttpResponse(200,
                "application/json", "{}");

        assertThrows(IllegalStateException.class,
                () -> store.saveCompleted("k", "hash", response, Duration.ofSeconds(30)));
    }
}
