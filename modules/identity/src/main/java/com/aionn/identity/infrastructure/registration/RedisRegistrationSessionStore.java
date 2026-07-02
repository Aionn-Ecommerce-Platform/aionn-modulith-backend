package com.aionn.identity.infrastructure.registration;

import com.aionn.identity.application.port.out.registration.RegistrationSessionStorePort;
import com.aionn.identity.domain.model.RegistrationVerificationSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisRegistrationSessionStore implements RegistrationSessionStorePort {

    private static final String REGISTRATION_SESSION_PREFIX = "identity:registration:session:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void save(RegistrationVerificationSession session) {
        String key = registrationSessionKey(session.getRegId());
        Duration ttl = calculateTtl(session.getExpiredAt());
        redisTemplate.opsForValue().set(key, session, ttl);
    }

    @Override
    public Optional<RegistrationVerificationSession> findByRegId(String regId) {
        Object value = redisTemplate.opsForValue().get(registrationSessionKey(regId));
        if (value instanceof RegistrationVerificationSession session) {
            return Optional.of(session);
        }
        return Optional.empty();
    }

    @Override
    public void deleteByRegId(String regId) {
        redisTemplate.delete(registrationSessionKey(regId));
    }

    private String registrationSessionKey(String regId) {
        return REGISTRATION_SESSION_PREFIX + regId;
    }

    private Duration calculateTtl(LocalDateTime expiredAt) {
        if (expiredAt == null) {
            return Duration.ofMinutes(5);
        }
        long seconds = Duration.between(LocalDateTime.now(), expiredAt).getSeconds();
        if (seconds <= 0) {
            return Duration.ofSeconds(1);
        }
        return Duration.ofSeconds(seconds);
    }
}
