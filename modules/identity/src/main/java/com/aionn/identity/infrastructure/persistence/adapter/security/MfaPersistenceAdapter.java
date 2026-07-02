package com.aionn.identity.infrastructure.persistence.adapter.security;

import com.aionn.identity.application.port.out.security.MfaPersistencePort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.infrastructure.persistence.entity.BackupCodeEntity;
import com.aionn.identity.infrastructure.persistence.repository.security.BackupCodeRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import com.aionn.identity.infrastructure.security.mfa.MfaSecretCipher;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MfaPersistenceAdapter implements MfaPersistencePort {

    private final UserRepository userRepository;
    private final BackupCodeRepository backupCodeRepository;
    private final MfaSecretCipher mfaSecretCipher;

    @Override
    public void updateMfaStatus(String userId, boolean enabled) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));
        user.setMfaEnabled(enabled);
        userRepository.save(user);
    }

    @Override
    public void saveMfaSecret(String userId, String secret) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));
        user.setMfaSecret(mfaSecretCipher.encrypt(secret));
        userRepository.save(user);
    }

    @Override
    public void clearMfa(String userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
        backupCodeRepository.deleteByUser_UserId(userId);
    }

    @Override
    public void deleteBackupCodes(String userId) {
        backupCodeRepository.deleteByUser_UserId(userId);
    }

    @Override
    public void saveBackupCodes(String userId, List<String> codeHashes) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));

        List<BackupCodeEntity> entities = codeHashes.stream()
                .map(codeHash -> BackupCodeEntity.builder()
                        .backupCodeId(IdGenerator.ulid())
                        .user(user)
                        .codeHash(codeHash)
                        .build())
                .toList();

        backupCodeRepository.saveAll(entities);
    }

    @Override
    public List<BackupCodeData> findActiveBackupCodes(String userId) {
        return backupCodeRepository.findByUser_UserIdAndUsedAtIsNullOrderByGeneratedAtDesc(userId).stream()
                .map(code -> new BackupCodeData(code.getBackupCodeId(), code.getCodeHash()))
                .toList();
    }

    @Override
    public boolean markBackupCodeUsed(String backupCodeId, LocalDateTime usedAt) {
        return backupCodeRepository.findById(backupCodeId)
                .map(code -> {
                    if (code.getUsedAt() != null) {
                        return false;
                    }
                    code.setUsedAt(usedAt);
                    backupCodeRepository.save(code);
                    return true;
                })
                .orElse(false);
    }
}
