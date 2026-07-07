package com.aionn.identity.infrastructure.registration;

import com.aionn.identity.domain.model.RegistrationVerificationSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRegistrationSessionStoreTest {

    private static final String KEY = "identity:registration:session:reg-1";

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisRegistrationSessionStore store;

    @BeforeEach
    void setUp() {
        store = new RedisRegistrationSessionStore(redisTemplate);
    }

    private RegistrationVerificationSession session(String regId, LocalDateTime expiredAt) {
        return new RegistrationVerificationSession(
                regId,
                "+84123456789",
                "123456",
                0,
                5,
                LocalDateTime.now().plusSeconds(30),
                expiredAt,
                false,
                null,
                null);
    }

    @Test
    void saveUsesTtlDerivedFromExpiry() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        RegistrationVerificationSession session = session("reg-1", LocalDateTime.now().plusMinutes(10));

        store.save(session);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.captor();
        verify(valueOperations).set(eq(KEY), eq(session), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isPositive();
    }

    @Test
    void saveUsesDefaultTtlWhenExpiryIsNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        RegistrationVerificationSession session = session("reg-1", null);

        store.save(session);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.captor();
        verify(valueOperations).set(eq(KEY), eq(session), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void saveUsesOneSecondTtlWhenAlreadyExpired() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        RegistrationVerificationSession session = session("reg-1", LocalDateTime.now().minusMinutes(10));

        store.save(session);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.captor();
        verify(valueOperations).set(eq(KEY), eq(session), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    void findReturnsSessionWhenPresent() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        RegistrationVerificationSession session = session("reg-1", LocalDateTime.now().plusMinutes(10));
        when(valueOperations.get(KEY)).thenReturn(session);

        Optional<RegistrationVerificationSession> found = store.findByRegId("reg-1");

        assertThat(found).contains(session);
    }

    @Test
    void findReturnsEmptyWhenNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(null);

        assertThat(store.findByRegId("reg-1")).isEmpty();
    }

    @Test
    void findReturnsEmptyWhenStoredTypeUnexpected() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn("not-a-session");

        assertThat(store.findByRegId("reg-1")).isEmpty();
    }

    @Test
    void deleteRemovesKey() {
        store.deleteByRegId("reg-1");

        verify(redisTemplate).delete(KEY);
    }
}
