package com.aionn.identity.infrastructure.security.otp;

import com.aionn.identity.application.port.out.user.UserOtpChallengeStorePort;
import com.aionn.identity.domain.valueobject.UserOtpPurpose;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@ConditionalOnProperty(prefix = "identity.account.otp-challenge", name = "provider", havingValue = "memory")
public class InMemoryUserOtpChallengeStore implements UserOtpChallengeStorePort {

    private final Map<String, UserOtpChallenge> storage = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> attempts = new ConcurrentHashMap<>();

    @Override
    public void save(UserOtpChallenge challenge) {
        if (challenge.attempts() == 0) {
            resetAttempts(challenge.userId(), challenge.purpose());
        }
        storage.put(key(challenge.userId(), challenge.purpose()), challenge);
    }

    @Override
    public Optional<UserOtpChallenge> find(String userId, UserOtpPurpose purpose) {
        return Optional.ofNullable(storage.get(key(userId, purpose)));
    }

    @Override
    public void delete(String userId, UserOtpPurpose purpose) {
        storage.remove(key(userId, purpose));
        attempts.remove(key(userId, purpose));
    }

    @Override
    public boolean tryAcquireAttempt(
            String userId, UserOtpPurpose purpose, int maxAttempts, Instant expiresAt) {
        int used = attempts.computeIfAbsent(key(userId, purpose), ignored -> new AtomicInteger())
                .incrementAndGet();
        return used <= maxAttempts;
    }

    @Override
    public void resetAttempts(String userId, UserOtpPurpose purpose) {
        attempts.remove(key(userId, purpose));
    }

    private String key(String userId, UserOtpPurpose purpose) {
        return userId + ":" + purpose.name();
    }
}
