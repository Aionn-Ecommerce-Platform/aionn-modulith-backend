package com.aionn.identity.infrastructure.auth.token;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRefreshTokenStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private SetOperations<String, String> setOperations;

    private RedisRefreshTokenStore store;

    @BeforeEach
    void setUp() {
        store = new RedisRefreshTokenStore(redisTemplate);
    }

    @Test
    void storeHashesTokenIdBeforePersisting() throws Exception {
        Duration ttl = Duration.ofMinutes(30);
        String tokenId = "refresh-token-id";
        String expectedHash = sha256(tokenId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        store.store(tokenId, "session-1", ttl);

        verify(valueOperations).set(
                eq("identity:auth:refresh:" + expectedHash),
                eq("session-1"),
                eq(ttl));
        verify(setOperations).add("identity:auth:refresh:session:session-1", expectedHash);
        verify(setOperations, never()).add(anyString(), eq(tokenId));
    }

    @Test
    void revokeBySessionIdDeletesOnlyHashedTokenKeys() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("identity:auth:refresh:session:session-1"))
                .thenReturn(Set.of("hash-a", "hash-b"));

        store.revokeBySessionId("session-1");

        verify(redisTemplate).delete("identity:auth:refresh:hash-a");
        verify(redisTemplate).delete("identity:auth:refresh:hash-b");
        verify(redisTemplate).delete("identity:auth:refresh:session:session-1");
    }

    private static String sha256(String value) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
