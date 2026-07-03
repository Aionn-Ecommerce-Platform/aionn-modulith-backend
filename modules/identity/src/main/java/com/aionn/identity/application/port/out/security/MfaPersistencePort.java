package com.aionn.identity.application.port.out.security;

import java.time.LocalDateTime;
import java.util.List;

public interface MfaPersistencePort {

    void updateMfaStatus(String userId, boolean enabled);

    void saveMfaSecret(String userId, String secret);

    void clearMfa(String userId);

    void deleteBackupCodes(String userId);

    void saveBackupCodes(String userId, List<String> codeHashes);

    List<BackupCodeData> findActiveBackupCodes(String userId);

    boolean markBackupCodeUsed(String backupCodeId, LocalDateTime usedAt);

    record BackupCodeData(String backupCodeId, String codeHash) {
    }
}
