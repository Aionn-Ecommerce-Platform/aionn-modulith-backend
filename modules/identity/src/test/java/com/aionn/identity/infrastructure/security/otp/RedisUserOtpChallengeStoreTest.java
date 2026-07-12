package com.aionn.identity.infrastructure.security.otp;

import com.aionn.identity.application.port.out.user.UserOtpChallengeStorePort.UserOtpChallenge;
import com.aionn.identity.domain.valueobject.OtpChannel;
import com.aionn.identity.domain.valueobject.UserOtpPurpose;
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
class RedisUserOtpChallengeStoreTest {

    private static final String KEY = "identity:otp:user-1:CHANGE_EMAIL";

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisUserOtpChallengeStore store;

    @BeforeEach
    void setUp() {
        store = new RedisUserOtpChallengeStore(redisTemplate);
    }

    @Test
    void saveThenFindRoundTripsAllFields() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        UserOtpChallenge challenge = new UserOtpChallenge(
                "user-1",
                UserOtpPurpose.CHANGE_EMAIL,
                OtpChannel.EMAIL,
                "new@example.com",
                "123456",
                "pending",
                Instant.now().plus(Duration.ofMinutes(5)),
                2);

        store.save(challenge);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.captor();
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.captor();
        verify(valueOperations).set(eq(KEY), valueCaptor.capture(), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isPositive();

        when(valueOperations.get(KEY)).thenReturn(valueCaptor.getValue());
        Optional<UserOtpChallenge> found = store.find("user-1", UserOtpPurpose.CHANGE_EMAIL);

        assertThat(found).contains(challenge);
    }

    @Test
    void saveThenFindRoundTripsNullableFieldsAsNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        UserOtpChallenge challenge = new UserOtpChallenge(
                "user-1",
                UserOtpPurpose.CHANGE_EMAIL,
                null,
                null,
                "123456",
                null,
                Instant.now().plus(Duration.ofMinutes(5)),
                0);

        store.save(challenge);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.captor();
        verify(valueOperations).set(eq(KEY), valueCaptor.capture(), any(Duration.class));

        when(valueOperations.get(KEY)).thenReturn(valueCaptor.getValue());
        Optional<UserOtpChallenge> found = store.find("user-1", UserOtpPurpose.CHANGE_EMAIL);

        assertThat(found).isPresent();
        assertThat(found.get().channel()).isNull();
        assertThat(found.get().target()).isNull();
        assertThat(found.get().pendingValue()).isNull();
    }

    @Test
    void saveFallsBackToDefaultTtlWhenChallengeAlreadyExpired() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        UserOtpChallenge challenge = new UserOtpChallenge(
                "user-1",
                UserOtpPurpose.CHANGE_EMAIL,
                OtpChannel.PHONE,
                "+84123",
                "999999",
                null,
                Instant.now().minus(Duration.ofDays(2)),
                0);

        store.save(challenge);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.captor();
        verify(valueOperations).set(eq(KEY), any(String.class), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void findReturnsEmptyWhenKeyMissing() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(null);

        assertThat(store.find("user-1", UserOtpPurpose.CHANGE_EMAIL)).isEmpty();
    }

    @Test
    void deleteRemovesKey() {
        store.delete("user-1", UserOtpPurpose.CHANGE_EMAIL);

        verify(redisTemplate).delete(KEY);
    }
}
