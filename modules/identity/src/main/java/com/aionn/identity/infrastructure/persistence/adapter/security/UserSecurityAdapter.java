package com.aionn.identity.infrastructure.persistence.adapter.security;

import com.aionn.identity.application.port.out.security.UserSecurityPort;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import com.aionn.identity.infrastructure.security.mfa.MfaSecretCipher;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserSecurityAdapter implements UserSecurityPort {

    private final UserRepository userRepository;
    private final MfaSecretCipher mfaSecretCipher;

    @Override
    public Optional<UserSecurityData> findById(String userId) {
        return userRepository.findById(userId)
                .map(user -> new UserSecurityData(
                        user.getUserId(),
                        user.getPasswordHash(),
                        user.getStatus(),
                        user.isMfaEnabled(),
                        mfaSecretCipher.decrypt(user.getMfaSecret()),
                        user.getLockedUntil(),
                        user.getFailedLoginAttempts()));
    }

    @Override
    public Optional<UserSecurityData> findByIdentity(String identity) {
        return userRepository.findByEmailIgnoreCase(identity)
                .or(() -> userRepository.findByPhone(identity))
                .or(() -> userRepository.findByUsernameIgnoreCase(identity))
                .map(user -> new UserSecurityData(
                        user.getUserId(),
                        user.getPasswordHash(),
                        user.getStatus(),
                        user.isMfaEnabled(),
                        mfaSecretCipher.decrypt(user.getMfaSecret()),
                        user.getLockedUntil(),
                        user.getFailedLoginAttempts()));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedLoginAttempt(String userId, int maxAttempts, Instant lockedUntil) {
        // The login transaction rolls back when the caller throws INVALID_CREDENTIALS, so the
        // counter must be committed in its own transaction to make lockout effective. The
        // increment itself is a single SQL statement to stay correct under concurrency.
        userRepository.incrementFailedLoginAttempts(userId, maxAttempts, lockedUntil);
    }

    @Override
    public void resetFailedLoginAttempts(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        });
    }
}
