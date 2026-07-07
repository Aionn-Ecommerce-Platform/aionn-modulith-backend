package com.aionn.identity.infrastructure.auth.token;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisTokenBlacklistTest {

    private static final String KEY_PREFIX = "identity:token-blacklist:";

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisTokenBlacklist blacklist;

    @BeforeEach
    void setUp() {
        blacklist = new RedisTokenBlacklist(redisTemplate);
    }

    @Test
    void blacklistStoresMarkerWithProvidedTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Duration ttl = Duration.ofMinutes(30);

        blacklist.blacklist("jti-1", ttl);

        verify(valueOperations).set(KEY_PREFIX + "jti-1", "1", ttl);
    }

    @Test
    void blacklistClampsTtlBelowMinimumToOneSecond() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        blacklist.blacklist("jti-1", Duration.ofMillis(100));

        verify(valueOperations).set(KEY_PREFIX + "jti-1", "1", Duration.ofSeconds(1));
    }

    @Test
    void blacklistUsesMinimumTtlWhenTtlIsNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        blacklist.blacklist("jti-1", null);

        verify(valueOperations).set(KEY_PREFIX + "jti-1", "1", Duration.ofSeconds(1));
    }

    @Test
    void blacklistIgnoresNullOrBlankJti() {
        blacklist.blacklist(null, Duration.ofMinutes(1));
        blacklist.blacklist("  ", Duration.ofMinutes(1));

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void isBlacklistedReturnsTrueWhenKeyPresent() {
        when(redisTemplate.hasKey(KEY_PREFIX + "jti-1")).thenReturn(true);

        assertThat(blacklist.isBlacklisted("jti-1")).isTrue();
    }

    @Test
    void isBlacklistedReturnsFalseWhenKeyMissing() {
        when(redisTemplate.hasKey(KEY_PREFIX + "jti-1")).thenReturn(false);

        assertThat(blacklist.isBlacklisted("jti-1")).isFalse();
    }

    @Test
    void isBlacklistedReturnsFalseWhenHasKeyReturnsNull() {
        when(redisTemplate.hasKey(KEY_PREFIX + "jti-1")).thenReturn(null);

        assertThat(blacklist.isBlacklisted("jti-1")).isFalse();
    }

    @Test
    void isBlacklistedReturnsFalseForNullOrBlankJti() {
        assertThat(blacklist.isBlacklisted(null)).isFalse();
        assertThat(blacklist.isBlacklisted("")).isFalse();

        verifyNoInteractions(redisTemplate);
    }
}
