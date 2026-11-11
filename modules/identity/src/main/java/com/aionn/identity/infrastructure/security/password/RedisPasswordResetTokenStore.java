package com.aionn.identity.infrastructure.security.password;

import com.aionn.sharedkernel.util.Sha256Hasher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisPasswordResetTokenStore {

    public record PasswordResetTokenData(String userId, Instant expiresAt) {
    }

    private static final String KEY_PREFIX = "identity:auth:password-reset:";
    private static final String USER_KEY_PREFIX = "identity:auth:password-reset:user:";

    /**
     * Consumes a token only while it is still the newest token issued for its user. Older
     * links stop working as soon as a new reset is requested, and the user pointer is
     * removed together with the token so the operation stays idempotent.
     */
    private static final RedisScript<String> CONSUME_IF_CURRENT_SCRIPT = new DefaultRedisScript<>(
            """
            local value = redis.call('GET', KEYS[1])
            if not value then
              return nil
            end
            redis.call('DEL', KEYS[1])
            local sep = string.find(value, ':', 1, true)
            if not sep then
              return nil
            end
            local userId = string.sub(value, 1, sep - 1)
            local userKey = ARGV[2] .. userId
            local current = redis.call('GET', userKey)
            if current == ARGV[1] then
              redis.call('DEL', userKey)
              return value
            end
            return nil
            """,
            String.class);

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;

    public void save(String token, String userId, Instant expiresAt) {
        Duration ttl = ttlFor(expiresAt);
        long epochSecond = expiresAt.getEpochSecond();
        String tokenHash = hash(token);

        String previousHash = redisTemplate.opsForValue().get(userKey(userId));
        if (previousHash != null && !previousHash.equals(tokenHash)) {
            redisTemplate.delete(KEY_PREFIX + previousHash);
        }
        redisTemplate.opsForValue().set(key(token), userId + ":" + epochSecond, ttl);
        redisTemplate.opsForValue().set(userKey(userId), tokenHash, ttl);
    }

    public Optional<PasswordResetTokenData> consume(String token) {
        String value = redisTemplate.execute(
                CONSUME_IF_CURRENT_SCRIPT,
                List.of(key(token)),
                hash(token),
                USER_KEY_PREFIX);
        return parse(value);
    }

    /** Revokes any outstanding reset link for a user, e.g. after a password change. */
    public void deleteForUser(String userId) {
        String tokenHash = redisTemplate.opsForValue().get(userKey(userId));
        if (tokenHash != null) {
            redisTemplate.delete(KEY_PREFIX + tokenHash);
            redisTemplate.delete(userKey(userId));
        }
    }

    private Duration ttlFor(Instant expiresAt) {
        Duration ttl = Duration.between(clock.instant(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            ttl = Duration.ofMinutes(1);
        }
        return ttl;
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
            Instant expiresAt = Instant.ofEpochSecond(epoch);
            return Optional.of(new PasswordResetTokenData(userId, expiresAt));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private static String key(String token) {
        return KEY_PREFIX + hash(token);
    }

    private static String userKey(String userId) {
        return USER_KEY_PREFIX + userId;
    }

    private static String hash(String token) {
        return Sha256Hasher.hexDigest(token);
    }
}
