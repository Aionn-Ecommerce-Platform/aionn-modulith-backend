package com.aionn.identity.infrastructure.security.password;

import java.time.Clock;

import com.aionn.identity.infrastructure.security.password.RedisPasswordResetTokenStore.PasswordResetTokenData;
import com.aionn.sharedkernel.util.Sha256Hasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisPasswordResetTokenStoreTest {

    private static final String TOKEN = "reset-token";
    private static final String TOKEN_HASH = Sha256Hasher.hexDigest(TOKEN);
    private static final String KEY = "identity:auth:password-reset:" + TOKEN_HASH;
    private static final String USER_KEY = "identity:auth:password-reset:user:user-1";

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisPasswordResetTokenStore store;

    @BeforeEach
    void setUp() {
        store = new RedisPasswordResetTokenStore(redisTemplate, Clock.systemUTC());
    }

    @Test
    void saveHashesTokenAndRegistersItAsTheCurrentToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(USER_KEY)).thenReturn(null);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(15));
        long epoch = expiresAt.getEpochSecond();

        store.save(TOKEN, "user-1", expiresAt);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.captor();
        verify(valueOperations).set(eq(KEY), eq("user-1:" + epoch), ttlCaptor.capture());
        verify(valueOperations).set(eq(USER_KEY), eq(TOKEN_HASH), ttlCaptor.capture());
        assertThat(ttlCaptor.getAllValues()).allMatch(Duration::isPositive);
    }

    @Test
    void saveInvalidatesThePreviouslyIssuedToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(USER_KEY)).thenReturn("old-hash");
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(15));

        store.save(TOKEN, "user-1", expiresAt);

        verify(redisTemplate).delete("identity:auth:password-reset:old-hash");
    }

    @Test
    void saveFallsBackToOneMinuteTtlWhenAlreadyExpired() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(USER_KEY)).thenReturn(null);
        Instant expiresAt = Instant.now().minus(Duration.ofMinutes(5));

        store.save(TOKEN, "user-1", expiresAt);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.captor();
        verify(valueOperations).set(eq(KEY), any(String.class), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void consumeOnlyAcceptsTheCurrentToken() {
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<String>>any(),
                org.mockito.ArgumentMatchers.<List<String>>any(),
                any(), any()))
                .thenReturn("user-1:1700000000");

        Optional<PasswordResetTokenData> data = store.consume(TOKEN);

        assertThat(data).isPresent();
        assertThat(data.get().userId()).isEqualTo("user-1");
    }

    @Test
    void consumeReturnsEmptyWhenScriptRejectsStaleOrMissingToken() {
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<String>>any(),
                org.mockito.ArgumentMatchers.<List<String>>any(),
                any(), any()))
                .thenReturn(null);

        assertThat(store.consume(TOKEN)).isEmpty();
    }

    @Test
    void deleteForUserRemovesTheCurrentTokenAndIndex() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(USER_KEY)).thenReturn(TOKEN_HASH);

        store.deleteForUser("user-1");

        verify(redisTemplate).delete(KEY);
        verify(redisTemplate).delete(USER_KEY);
    }

    @Test
    void deleteForUserIsANoOpWhenNoTokenExists() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(USER_KEY)).thenReturn(null);

        store.deleteForUser("user-1");

        verify(redisTemplate, never()).delete(any(String.class));
    }
}
