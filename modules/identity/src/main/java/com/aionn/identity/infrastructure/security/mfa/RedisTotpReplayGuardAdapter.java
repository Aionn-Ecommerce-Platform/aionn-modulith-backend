package com.aionn.identity.infrastructure.security.mfa;

import com.aionn.identity.application.port.out.security.TotpReplayGuardPort;
import com.aionn.sharedkernel.util.Sha256Hasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis-backed TOTP replay guard.
 *
 * <p>Keys expire after two minutes, which covers the accepted time step plus the configured
 * clock-skew window, so the guard never needs an explicit cleanup job.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisTotpReplayGuardAdapter implements TotpReplayGuardPort {

    private static final String KEY_PREFIX = "identity:auth:totp-used:";
    private static final Duration RETENTION = Duration.ofMinutes(2);

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean claim(String userId, String code) {
        if (userId == null || code == null || code.isBlank()) {
            return false;
        }
        String key = KEY_PREFIX + userId + ":" + Sha256Hasher.hexDigest(code);
        try {
            return Boolean.TRUE.equals(
                    redisTemplate.opsForValue().setIfAbsent(key, "1", RETENTION));
        } catch (RuntimeException ex) {
            // Fail closed: if we cannot prove the code is fresh we must not accept it.
            log.warn("TOTP replay guard unavailable; rejecting code for user {}", userId, ex);
            return false;
        }
    }
}
