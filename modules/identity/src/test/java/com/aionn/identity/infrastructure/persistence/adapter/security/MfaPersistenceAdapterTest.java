package com.aionn.identity.infrastructure.persistence.adapter.security;

import com.aionn.identity.application.port.out.security.MfaPersistencePort.BackupCodeData;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.infrastructure.persistence.entity.BackupCodeEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.repository.security.BackupCodeRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import com.aionn.identity.infrastructure.security.mfa.MfaSecretCipher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MfaPersistenceAdapterTest {

    private static final String USER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    @Mock
    private UserRepository userRepository;
    @Mock
    private BackupCodeRepository backupCodeRepository;
    @Mock
    private MfaSecretCipher mfaSecretCipher;

    @InjectMocks
    private MfaPersistenceAdapter adapter;

    @Test
    void updateMfaStatusEnablesAndSaves() {
        UserEntity user = mock(UserEntity.class);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        adapter.updateMfaStatus(USER_ID, true);

        verify(user).setMfaEnabled(true);
        verify(userRepository).save(user);
    }

    @Test
    void updateMfaStatusThrowsWhenUserMissing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.updateMfaStatus(USER_ID, true))
                .isInstanceOfSatisfying(IdentityException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.USER_NOT_FOUND.getCode()));
    }

    @Test
    void saveMfaSecretEncryptsBeforeStoring() {
        UserEntity user = mock(UserEntity.class);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(mfaSecretCipher.encrypt("secret")).thenReturn("encrypted");

        adapter.saveMfaSecret(USER_ID, "secret");

        verify(user).setMfaSecret("encrypted");
        verify(userRepository).save(user);
    }

    @Test
    void saveMfaSecretThrowsWhenUserMissing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.saveMfaSecret(USER_ID, "secret"))
                .isInstanceOfSatisfying(IdentityException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.USER_NOT_FOUND.getCode()));
    }

    @Test
    void clearMfaResetsUserFlagsAndDeletesBackupCodes() {
        UserEntity user = mock(UserEntity.class);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        adapter.clearMfa(USER_ID);

        verify(user).setMfaEnabled(false);
        verify(user).setMfaSecret(null);
        verify(userRepository).save(user);
        verify(backupCodeRepository).deleteByUser_UserId(USER_ID);
    }

    @Test
    void clearMfaThrowsWhenUserMissing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.clearMfa(USER_ID))
                .isInstanceOfSatisfying(IdentityException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.USER_NOT_FOUND.getCode()));
        verify(backupCodeRepository, never()).deleteByUser_UserId(any());
    }

    @Test
    void deleteBackupCodesDelegates() {
        adapter.deleteBackupCodes(USER_ID);

        verify(backupCodeRepository).deleteByUser_UserId(USER_ID);
    }

    @Test
    void saveBackupCodesPersistsOneEntityPerHash() {
        UserEntity user = mock(UserEntity.class);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        adapter.saveBackupCodes(USER_ID, List.of("h1", "h2", "h3"));

        ArgumentCaptor<List<BackupCodeEntity>> captor = ArgumentCaptor.captor();
        verify(backupCodeRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(3);
        assertThat(captor.getValue()).extracting(BackupCodeEntity::getCodeHash)
                .containsExactly("h1", "h2", "h3");
    }

    @Test
    void saveBackupCodesThrowsWhenUserMissing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.saveBackupCodes(USER_ID, List.of("h1")))
                .isInstanceOfSatisfying(IdentityException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.USER_NOT_FOUND.getCode()));
        verify(backupCodeRepository, never()).saveAll(anyList());
    }

    @Test
    void findActiveBackupCodesMapsToData() {
        BackupCodeEntity entity = BackupCodeEntity.builder()
                .backupCodeId("01HZBCK0000000000000000001")
                .codeHash("hash-1")
                .build();
        when(backupCodeRepository.findByUser_UserIdAndUsedAtIsNullOrderByGeneratedAtDesc(USER_ID))
                .thenReturn(List.of(entity));

        List<BackupCodeData> result = adapter.findActiveBackupCodes(USER_ID);

        assertThat(result).containsExactly(new BackupCodeData("01HZBCK0000000000000000001", "hash-1"));
    }

    @Test
    void markBackupCodeUsedReturnsTrueWhenRowUpdated() {
        Instant usedAt = Instant.now();
        when(backupCodeRepository.markAsUsedIfUnused("code-1", usedAt)).thenReturn(1);

        assertThat(adapter.markBackupCodeUsed("code-1", usedAt)).isTrue();
    }

    @Test
    void markBackupCodeUsedReturnsFalseWhenNoRowUpdated() {
        Instant usedAt = Instant.now();
        when(backupCodeRepository.markAsUsedIfUnused("code-1", usedAt)).thenReturn(0);

        assertThat(adapter.markBackupCodeUsed("code-1", usedAt)).isFalse();
    }
}
