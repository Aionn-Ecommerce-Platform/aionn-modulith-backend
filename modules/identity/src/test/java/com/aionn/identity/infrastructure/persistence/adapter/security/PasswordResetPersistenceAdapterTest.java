package com.aionn.identity.infrastructure.persistence.adapter.security;

import com.aionn.identity.application.port.out.security.PasswordResetPort.PasswordResetTokenData;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import com.aionn.identity.infrastructure.security.password.RedisPasswordResetTokenStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetPersistenceAdapterTest {

    private static final String USER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String TOKEN_HASH = "token-hash";

    @Mock
    private UserRepository userRepository;
    @Mock
    private RedisPasswordResetTokenStore tokenStore;

    @InjectMocks
    private PasswordResetPersistenceAdapter adapter;

    @Test
    void savePasswordResetTokenHashDelegatesToStore() {
        Instant expiresAt = Instant.now().plus(Duration.ofHours(1));

        adapter.savePasswordResetTokenHash(TOKEN_HASH, USER_ID, expiresAt);

        verify(tokenStore).save(TOKEN_HASH, USER_ID, expiresAt);
    }

    @Test
    void findPasswordResetTokenHashReturnsDataWhenPresent() {
        Instant expiresAt = Instant.now().plus(Duration.ofHours(1));
        when(tokenStore.find(TOKEN_HASH))
                .thenReturn(Optional.of(new RedisPasswordResetTokenStore.PasswordResetTokenData(USER_ID, expiresAt)));

        assertThat(adapter.findPasswordResetTokenHash(TOKEN_HASH))
                .contains(new PasswordResetTokenData(USER_ID, expiresAt));
    }

    @Test
    void findPasswordResetTokenHashReturnsEmptyWhenMissing() {
        when(tokenStore.find(TOKEN_HASH)).thenReturn(Optional.empty());

        assertThat(adapter.findPasswordResetTokenHash(TOKEN_HASH)).isEmpty();
    }

    @Test
    void consumePasswordResetTokenHashReturnsDataWhenPresent() {
        Instant expiresAt = Instant.now().plus(Duration.ofHours(1));
        when(tokenStore.consume(TOKEN_HASH))
                .thenReturn(Optional.of(new RedisPasswordResetTokenStore.PasswordResetTokenData(USER_ID, expiresAt)));

        assertThat(adapter.consumePasswordResetTokenHash(TOKEN_HASH))
                .contains(new PasswordResetTokenData(USER_ID, expiresAt));
    }

    @Test
    void consumePasswordResetTokenHashReturnsEmptyWhenMissing() {
        when(tokenStore.consume(TOKEN_HASH)).thenReturn(Optional.empty());

        assertThat(adapter.consumePasswordResetTokenHash(TOKEN_HASH)).isEmpty();
    }

    @Test
    void deletePasswordResetTokenHashDelegatesToStore() {
        adapter.deletePasswordResetTokenHash(TOKEN_HASH);

        verify(tokenStore).delete(TOKEN_HASH);
    }

    @Test
    void updatePasswordUpdatesHashAndSaves() {
        UserEntity user = mock(UserEntity.class);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        adapter.updatePassword(USER_ID, "new-hash");

        verify(user).setPasswordHash("new-hash");
        verify(userRepository).save(user);
    }

    @Test
    void updatePasswordThrowsWhenUserMissing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.updatePassword(USER_ID, "new-hash"))
                .isInstanceOfSatisfying(IdentityException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.USER_NOT_FOUND.getCode()));
    }
}
