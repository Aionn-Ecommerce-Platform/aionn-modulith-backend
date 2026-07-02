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
    public void savePasswordResetToken(String token, String userId, LocalDateTime expiresAt) {
        tokenStore.save(token, userId, expiresAt);
    }

    @Override
    public Optional<PasswordResetTokenData> findPasswordResetToken(String token) {
        return tokenStore.find(token)
                .map(data -> new PasswordResetTokenData(data.userId(), data.expiresAt()));
    }

    @Override
    public Optional<PasswordResetTokenData> consumePasswordResetToken(String token) {
        return tokenStore.consume(token)
                .map(data -> new PasswordResetTokenData(data.userId(), data.expiresAt()));
    }

    @Override
    public void deletePasswordResetToken(String token) {
        tokenStore.delete(token);
    }

    @Override
    public void updatePassword(String userId, String passwordHash) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));
        user.setPasswordHash(passwordHash);
        userRepository.save(user);
    }
}
