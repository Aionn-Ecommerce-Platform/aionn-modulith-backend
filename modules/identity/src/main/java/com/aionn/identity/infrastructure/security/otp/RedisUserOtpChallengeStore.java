package com.aionn.identity.infrastructure.security.otp;

import com.aionn.identity.application.port.out.user.UserOtpChallengeStorePort;
import com.aionn.identity.domain.valueobject.OtpChannel;
import com.aionn.identity.domain.valueobject.UserOtpPurpose;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "identity.account.otp-challenge", name = "provider", havingValue = "redis", matchIfMissing = true)
@RequiredArgsConstructor
public class RedisUserOtpChallengeStore implements UserOtpChallengeStorePort {

    private static final String KEY_PREFIX = "identity:otp:";
    private static final String ATTEMPT_KEY_PREFIX = "identity:otp-attempts:";
    private static final String DELIMITER = "|";
    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ISO_INSTANT;
    private static final Duration EXPIRY_BUFFER = Duration.ofMinutes(2);

    private static final RedisScript<Long> ACQUIRE_ATTEMPT_SCRIPT = new DefaultRedisScript<>(
            """
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
              redis.call('PEXPIRE', KEYS[1], ARGV[2])
            end
            if count > tonumber(ARGV[1]) then
              return 0
            end
            return 1
            """,
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;

    @Override
    public void save(UserOtpChallenge challenge) {
        String key = key(challenge.userId(), challenge.purpose());
        String value = serialize(challenge);

        Duration ttl = Duration.between(clock.instant(), challenge.expiresAt())
                .plus(EXPIRY_BUFFER);
        if (ttl.isNegative() || ttl.isZero()) {
            ttl = Duration.ofMinutes(10);
        }

        if (challenge.attempts() == 0) {
            resetAttempts(challenge.userId(), challenge.purpose());
        }
        redisTemplate.opsForValue().set(key, value, ttl);
        log.debug("Saved OTP challenge: userId={}, purpose={}, ttl={}s",
                challenge.userId(), challenge.purpose(), ttl.getSeconds());
    }

    @Override
    public Optional<UserOtpChallenge> find(String userId, UserOtpPurpose purpose) {
        String key = key(userId, purpose);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(deserialize(value));
    }

    @Override
    public void delete(String userId, UserOtpPurpose purpose) {
        redisTemplate.delete(key(userId, purpose));
        resetAttempts(userId, purpose);
        log.debug("Deleted OTP challenge: userId={}, purpose={}", userId, purpose);
    }

    @Override
    public boolean tryAcquireAttempt(
            String userId, UserOtpPurpose purpose, int maxAttempts, Instant expiresAt) {
        long ttlMillis = expiresAt == null
                ? Duration.ofMinutes(10).toMillis()
                : Math.max(Duration.between(clock.instant(), expiresAt).toMillis(), 1000L);
        Long allowed = redisTemplate.execute(
                ACQUIRE_ATTEMPT_SCRIPT,
                List.of(attemptKey(userId, purpose)),
                Integer.toString(maxAttempts),
                Long.toString(ttlMillis));
        return allowed != null && allowed == 1L;
    }

    @Override
    public void resetAttempts(String userId, UserOtpPurpose purpose) {
        redisTemplate.delete(attemptKey(userId, purpose));
    }

    private String key(String userId, UserOtpPurpose purpose) {
        return KEY_PREFIX + userId + ":" + purpose.name();
    }

    private String attemptKey(String userId, UserOtpPurpose purpose) {
        return ATTEMPT_KEY_PREFIX + userId + ":" + purpose.name();
    }

    private String serialize(UserOtpChallenge c) {
        return String.join(DELIMITER,
                c.userId(),
                c.purpose().name(),
                c.channel() != null ? c.channel().name() : "",
                c.target() != null ? c.target() : "",
                c.otpCode(),
                c.pendingValue() != null ? c.pendingValue() : "",
                DT_FORMAT.format(c.expiresAt()),
                String.valueOf(c.attempts()));
    }

    private UserOtpChallenge deserialize(String value) {
        String[] parts = value.split("\\|", -1);
        return new UserOtpChallenge(
                parts[0],
                UserOtpPurpose.valueOf(parts[1]),
                parts[2].isEmpty() ? null : OtpChannel.valueOf(parts[2]),
                parts[3].isEmpty() ? null : parts[3],
                parts[4],
                parts[5].isEmpty() ? null : parts[5],
                Instant.from(DT_FORMAT.parse(parts[6])),
                Integer.parseInt(parts[7]));
    }
}
