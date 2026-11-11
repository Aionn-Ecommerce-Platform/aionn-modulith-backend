package com.aionn.identity.infrastructure.registration;

import com.aionn.identity.application.port.out.registration.RegistrationSessionStorePort;
import com.aionn.identity.domain.model.RegistrationVerificationSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
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
public class RedisRegistrationSessionStore implements RegistrationSessionStorePort {

    private static final String REGISTRATION_SESSION_PREFIX = "identity:registration:session:";
    private static final String VERIFY_ATTEMPTS_PREFIX = "identity:registration:verify-attempts:";

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

    private final RedisTemplate<String, Object> redisTemplate;
    private final Clock clock;

    @Override
    public void save(RegistrationVerificationSession session) {
        String key = registrationSessionKey(session.getRegId());
        Duration ttl = calculateTtl(session.getExpiredAt());
        redisTemplate.opsForValue().set(key, RegistrationSessionDocument.from(session), ttl);
    }

    @Override
    public Optional<RegistrationVerificationSession> findByRegId(String regId) {
        Object value = redisTemplate.opsForValue().get(registrationSessionKey(regId));
        if (value instanceof RegistrationSessionDocument document) {
            return Optional.of(document.toDomain());
        }
        return Optional.empty();
    }

    @Override
    public void deleteByRegId(String regId) {
        redisTemplate.delete(registrationSessionKey(regId));
        resetVerifyAttempts(regId);
    }

    @Override
    public boolean tryAcquireVerifyAttempt(String regId, int maxAttempts, Instant expiresAt) {
        Duration ttl = calculateTtl(expiresAt);
        Long allowed = redisTemplate.execute(
                ACQUIRE_ATTEMPT_SCRIPT,
                List.of(verifyAttemptsKey(regId)),
                Integer.toString(maxAttempts),
                Long.toString(Math.max(ttl.toMillis(), 1000L)));
        return allowed != null && allowed == 1L;
    }

    @Override
    public void resetVerifyAttempts(String regId) {
        redisTemplate.delete(verifyAttemptsKey(regId));
    }

    private String registrationSessionKey(String regId) {
        return REGISTRATION_SESSION_PREFIX + regId;
    }

    private String verifyAttemptsKey(String regId) {
        return VERIFY_ATTEMPTS_PREFIX + regId;
    }

    private Duration calculateTtl(Instant expiredAt) {
        if (expiredAt == null) {
            return Duration.ofMinutes(5);
        }
        long seconds = Duration.between(clock.instant(), expiredAt).getSeconds();
        if (seconds <= 0) {
            return Duration.ofSeconds(1);
        }
        return Duration.ofSeconds(seconds);
    }
}
