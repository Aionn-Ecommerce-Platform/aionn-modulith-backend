package com.aionn.identity.infrastructure.registration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRegistrationLockManagerTest {

    private static final String REDIS_KEY = "identity:registration:lock:phone";

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisRegistrationLockManager lockManager;

    @BeforeEach
    void setUp() {
        lockManager = new RedisRegistrationLockManager(redisTemplate);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void tryLockReturnsTokenWhenAcquired() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(REDIS_KEY), any(String.class), eq(Duration.ofSeconds(30))))
                .thenReturn(true);

        Optional<String> token = lockManager.tryLock("phone", 30);

        assertThat(token).isPresent();
    }

    @Test
    void tryLockReturnsEmptyWhenNotAcquired() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(REDIS_KEY), any(String.class), any(Duration.class)))
                .thenReturn(false);

        assertThat(lockManager.tryLock("phone", 30)).isEmpty();
    }

    @Test
    void unlockRunsReleaseScriptWithToken() {
        lockManager.unlock("phone", "token-1");

        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.captor();
        verify(redisTemplate).execute(any(RedisScript.class), keysCaptor.capture(), eq("token-1"));
        assertThat(keysCaptor.getValue()).containsExactly(REDIS_KEY);
    }

    @Test
    void unlockIgnoresNullOrEmptyToken() {
        lockManager.unlock("phone", null);
        lockManager.unlock("phone", "");

        verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), any());
    }

    @Test
    void unlockAfterCompletionRunsImmediatelyWithoutActiveTransaction() {
        lockManager.unlockAfterCompletion("phone", "token-1");

        verify(redisTemplate).execute(any(RedisScript.class), anyList(), eq("token-1"));
    }

    @Test
    void unlockAfterCompletionDefersWhenTransactionActive() {
        TransactionSynchronizationManager.initSynchronization();

        lockManager.unlockAfterCompletion("phone", "token-1");

        verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), any());
        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
    }

    @Test
    void unlockAfterCompletionIgnoresNullToken() {
        lockManager.unlockAfterCompletion("phone", null);

        verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), any());
    }
}
