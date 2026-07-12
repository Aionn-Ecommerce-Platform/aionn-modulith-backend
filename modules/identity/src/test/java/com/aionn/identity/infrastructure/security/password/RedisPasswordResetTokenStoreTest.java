package com.aionn.identity.infrastructure.security.password;

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

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisPasswordResetTokenStoreTest {

    private static final String TOKEN = "reset-token";
    private static final String KEY = "identity:auth:password-reset:" + Sha256Hasher.hexDigest(TOKEN);

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisPasswordResetTokenStore store;

    @BeforeEach
    void setUp() {
        store = new RedisPasswordResetTokenStore(redisTemplate);
    }

    @Test
    void saveHashesTokenAndPersistsUserIdWithEpoch() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(15));
        long epoch = expiresAt.getEpochSecond();

        store.save(TOKEN, "user-1", expiresAt);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.captor();
        verify(valueOperations).set(eq(KEY), eq("user-1:" + epoch), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isPositive();
    }

    @Test
    void saveFallsBackToOneMinuteTtlWhenAlreadyExpired() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Instant expiresAt = Instant.now().minus(Duration.ofMinutes(5));

        store.save(TOKEN, "user-1", expiresAt);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.captor();
        verify(valueOperations).set(eq(KEY), any(String.class), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void findParsesStoredValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        long epoch = 1_700_000_000L;
        when(valueOperations.get(KEY)).thenReturn("user-1:" + epoch);

        Optional<PasswordResetTokenData> data = store.find(TOKEN);

        assertThat(data).isPresent();
        assertThat(data.get().userId()).isEqualTo("user-1");
        assertThat(data.get().expiresAt()).isEqualTo(Instant.ofEpochSecond(epoch));
    }

    @Test
    void findReturnsEmptyWhenValueMissing() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(null);

        assertThat(store.find(TOKEN)).isEmpty();
    }

    @Test
    void findReturnsEmptyWhenSeparatorMissing() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn("no-separator");

        assertThat(store.find(TOKEN)).isEmpty();
    }

    @Test
    void findReturnsEmptyWhenEpochNotNumeric() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn("user-1:not-a-number");

        assertThat(store.find(TOKEN)).isEmpty();
    }

    @Test
    void consumeReadsAndDeletesAtomically() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        long epoch = 1_700_000_000L;
        when(valueOperations.getAndDelete(KEY)).thenReturn("user-1:" + epoch);

        Optional<PasswordResetTokenData> data = store.consume(TOKEN);

        assertThat(data).isPresent();
        assertThat(data.get().userId()).isEqualTo("user-1");
    }

    @Test
    void deleteRemovesHashedKey() {
        store.delete(TOKEN);

        verify(redisTemplate).delete(KEY);
    }
}
