package com.aionn.identity.infrastructure.security.password;

import com.aionn.sharedkernel.util.Sha256Hasher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisPasswordResetTokenStore {

    public record PasswordResetTokenData(String userId, LocalDateTime expiresAt) {
    }

    private static final String KEY_PREFIX = "identity:auth:password-reset:";

    private final StringRedisTemplate redisTemplate;

    public void save(String token, String userId, LocalDateTime expiresAt) {
        // Callers pass expiresAt in UTC (PasswordResetService uses ZoneOffset.UTC).
        // Compare and serialize consistently in UTC so non-UTC hosts do not
        // skew TTLs or persisted expiry timestamps.
        Duration ttl = Duration.between(Instant.now(), expiresAt.toInstant(ZoneOffset.UTC));
        if (ttl.isNegative() || ttl.isZero()) {
            ttl = Duration.ofMinutes(1);
        }
        long epochSecond = expiresAt.toEpochSecond(ZoneOffset.UTC);
        redisTemplate.opsForValue().set(key(token), userId + ":" + epochSecond, ttl);
    }

    public Optional<PasswordResetTokenData> find(String token) {
        return parse(redisTemplate.opsForValue().get(key(token)));
    }

    public Optional<PasswordResetTokenData> consume(String token) {
        return parse(redisTemplate.opsForValue().getAndDelete(key(token)));
    }

    private Optional<PasswordResetTokenData> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        int sep = value.lastIndexOf(':');
        if (sep < 0) {
            return Optional.empty();
        }
        try {
            long epoch = Long.parseLong(value.substring(sep + 1));
            String userId = value.substring(0, sep);
            LocalDateTime expiresAt = LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.UTC);
            return Optional.of(new PasswordResetTokenData(userId, expiresAt));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public void delete(String token) {
        redisTemplate.delete(key(token));
    }

    private static String key(String token) {
        return KEY_PREFIX + Sha256Hasher.hexDigest(token);
    }
}
