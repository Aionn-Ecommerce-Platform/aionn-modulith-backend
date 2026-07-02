package com.aionn.identity.infrastructure.persistence.adapter.security;

import com.aionn.identity.application.port.out.security.PasswordResetPort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import com.aionn.identity.infrastructure.security.password.RedisPasswordResetTokenStore;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PasswordResetPersistenceAdapter implements PasswordResetPort {

    private final UserRepository userRepository;
    private final RedisPasswordResetTokenStore tokenStore;

    @Override
    public void savePasswordResetTokenHash(String tokenHash, String userId, LocalDateTime expiresAt) {
        tokenStore.save(tokenHash, userId, expiresAt);
    }

    @Override
    public Optional<PasswordResetTokenData> findPasswordResetTokenHash(String tokenHash) {
        return tokenStore.find(tokenHash)
                .map(data -> new PasswordResetTokenData(data.userId(), data.expiresAt()));
    }

    @Override
    public Optional<PasswordResetTokenData> consumePasswordResetTokenHash(String tokenHash) {
        return tokenStore.consume(tokenHash)
                .map(data -> new PasswordResetTokenData(data.userId(), data.expiresAt()));
    }

    @Override
    public void deletePasswordResetTokenHash(String tokenHash) {
        tokenStore.delete(tokenHash);
    }

    @Override
    public void updatePassword(String userId, String passwordHash) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));
        user.setPasswordHash(passwordHash);
        userRepository.save(user);
    }
}
