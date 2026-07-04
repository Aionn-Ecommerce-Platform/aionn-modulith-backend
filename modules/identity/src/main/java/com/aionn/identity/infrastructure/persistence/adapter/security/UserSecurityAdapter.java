package com.aionn.identity.infrastructure.persistence.adapter.security;

import com.aionn.identity.application.port.out.security.UserSecurityPort;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import com.aionn.identity.infrastructure.security.mfa.MfaSecretCipher;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
    public void recordFailedLoginAttempt(String userId, int failedAttempts, LocalDateTime lockedUntil) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setFailedLoginAttempts(failedAttempts);
            user.setLockedUntil(lockedUntil);
            userRepository.save(user);
        });
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
