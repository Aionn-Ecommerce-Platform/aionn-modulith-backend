package com.aionn.identity.infrastructure.security.mfa;

import com.aionn.sharedkernel.util.Sha256Hasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisTotpReplayGuardAdapterTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisTotpReplayGuardAdapter guard;

    @BeforeEach
    void setUp() {
        guard = new RedisTotpReplayGuardAdapter(redisTemplate);
    }

    @Test
    void firstClaimOfACodeSucceeds() {
        String key = "identity:auth:totp-used:user-1:" + Sha256Hasher.hexDigest("123456");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(key), eq("1"), eq(Duration.ofMinutes(2))))
                .thenReturn(true);

        assertThat(guard.claim("user-1", "123456")).isTrue();
    }

    @Test
    void secondClaimOfTheSameCodeIsRejected() {
        String key = "identity:auth:totp-used:user-1:" + Sha256Hasher.hexDigest("123456");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(key), eq("1"), eq(Duration.ofMinutes(2))))
                .thenReturn(false);

        assertThat(guard.claim("user-1", "123456")).isFalse();
    }

    @Test
    void differentCodesAreTrackedIndependently() {
        String firstKey = "identity:auth:totp-used:user-1:" + Sha256Hasher.hexDigest("123456");
        String secondKey = "identity:auth:totp-used:user-1:" + Sha256Hasher.hexDigest("654321");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(firstKey), eq("1"), eq(Duration.ofMinutes(2))))
                .thenReturn(true);
        when(valueOperations.setIfAbsent(eq(secondKey), eq("1"), eq(Duration.ofMinutes(2))))
                .thenReturn(true);

        assertThat(guard.claim("user-1", "123456")).isTrue();
        assertThat(guard.claim("user-1", "654321")).isTrue();
    }

    @Test
    void failsClosedWhenRedisIsUnavailable() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(
                eq("identity:auth:totp-used:user-1:" + Sha256Hasher.hexDigest("123456")),
                eq("1"), eq(Duration.ofMinutes(2))))
                .thenThrow(new RedisConnectionFailureException("down"));

        assertThat(guard.claim("user-1", "123456")).isFalse();
    }

    @Test
    void blankInputsAreRejectedWithoutTouchingRedis() {
        assertThat(guard.claim(null, "123456")).isFalse();
        assertThat(guard.claim("user-1", null)).isFalse();
        assertThat(guard.claim("user-1", "  ")).isFalse();
    }
}
