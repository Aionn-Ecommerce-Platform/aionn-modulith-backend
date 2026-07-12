package com.aionn.identity.infrastructure.persistence.adapter.security;

import com.aionn.identity.application.port.out.security.UserSecurityPort.UserSecurityData;
import com.aionn.identity.domain.valueobject.UserStatus;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import com.aionn.identity.infrastructure.security.mfa.MfaSecretCipher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSecurityAdapterTest {

    private static final String USER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    @Mock
    private UserRepository userRepository;
    @Mock
    private MfaSecretCipher mfaSecretCipher;

    @InjectMocks
    private UserSecurityAdapter adapter;

    private UserEntity securityUser() {
        UserEntity user = mock(UserEntity.class);
        lenient().when(user.getUserId()).thenReturn(USER_ID);
        lenient().when(user.getPasswordHash()).thenReturn("hash");
        lenient().when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        lenient().when(user.isMfaEnabled()).thenReturn(true);
        lenient().when(user.getMfaSecret()).thenReturn("cipher");
        lenient().when(user.getLockedUntil()).thenReturn(null);
        lenient().when(user.getFailedLoginAttempts()).thenReturn(2);
        return user;
    }

    @Test
    void findByIdMapsUserSecurityDataAndDecryptsSecret() {
        UserEntity user = securityUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(mfaSecretCipher.decrypt("cipher")).thenReturn("plain");

        Optional<UserSecurityData> result = adapter.findById(USER_ID);

        assertThat(result).hasValueSatisfying(data -> {
            assertThat(data.userId()).isEqualTo(USER_ID);
            assertThat(data.passwordHash()).isEqualTo("hash");
            assertThat(data.status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(data.mfaEnabled()).isTrue();
            assertThat(data.mfaSecret()).isEqualTo("plain");
            assertThat(data.failedLoginAttempts()).isEqualTo(2);
        });
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThat(adapter.findById(USER_ID)).isEmpty();
        verify(mfaSecretCipher, never()).decrypt(any());
    }

    @Test
    void findByIdentityResolvesByEmail() {
        UserEntity user = securityUser();
        when(userRepository.findByEmailIgnoreCase("id")).thenReturn(Optional.of(user));
        when(mfaSecretCipher.decrypt("cipher")).thenReturn("plain");

        assertThat(adapter.findByIdentity("id"))
                .hasValueSatisfying(data -> assertThat(data.userId()).isEqualTo(USER_ID));
    }

    @Test
    void findByIdentityFallsBackToPhone() {
        UserEntity user = securityUser();
        when(userRepository.findByEmailIgnoreCase("id")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("id")).thenReturn(Optional.of(user));
        when(mfaSecretCipher.decrypt("cipher")).thenReturn("plain");

        assertThat(adapter.findByIdentity("id"))
                .hasValueSatisfying(data -> assertThat(data.userId()).isEqualTo(USER_ID));
    }

    @Test
    void findByIdentityFallsBackToUsername() {
        UserEntity user = securityUser();
        when(userRepository.findByEmailIgnoreCase("id")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("id")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameIgnoreCase("id")).thenReturn(Optional.of(user));
        when(mfaSecretCipher.decrypt("cipher")).thenReturn("plain");

        assertThat(adapter.findByIdentity("id"))
                .hasValueSatisfying(data -> assertThat(data.userId()).isEqualTo(USER_ID));
    }

    @Test
    void findByIdentityReturnsEmptyWhenNoMatch() {
        when(userRepository.findByEmailIgnoreCase("id")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("id")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameIgnoreCase("id")).thenReturn(Optional.empty());

        assertThat(adapter.findByIdentity("id")).isEmpty();
        verify(mfaSecretCipher, never()).decrypt(any());
    }

    @Test
    void recordFailedLoginAttemptUpdatesUserWhenPresent() {
        UserEntity user = mock(UserEntity.class);
        Instant lockedUntil = Instant.now().plus(Duration.ofMinutes(15));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        adapter.recordFailedLoginAttempt(USER_ID, 3, lockedUntil);

        verify(user).setFailedLoginAttempts(3);
        verify(user).setLockedUntil(lockedUntil);
        verify(userRepository).save(user);
    }

    @Test
    void recordFailedLoginAttemptDoesNothingWhenMissing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        adapter.recordFailedLoginAttempt(USER_ID, 3, Instant.now());

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetFailedLoginAttemptsClearsCountersWhenPresent() {
        UserEntity user = mock(UserEntity.class);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        adapter.resetFailedLoginAttempts(USER_ID);

        verify(user).setFailedLoginAttempts(0);
        verify(user).setLockedUntil(null);
        verify(userRepository).save(user);
    }

    @Test
    void resetFailedLoginAttemptsDoesNothingWhenMissing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        adapter.resetFailedLoginAttempts(USER_ID);

        verify(userRepository, never()).save(any());
    }
}
