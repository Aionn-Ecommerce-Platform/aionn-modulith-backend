package com.aionn.identity.infrastructure.persistence.repository.security;

import com.aionn.identity.infrastructure.persistence.entity.BackupCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface BackupCodeRepository extends JpaRepository<BackupCodeEntity, String> {

    List<BackupCodeEntity> findByUser_UserIdAndUsedAtIsNullOrderByGeneratedAtDesc(String userId);

    void deleteByUser_UserId(String userId);

    // Conditional update that only claims a still-unused backup code; the WHERE
    // usedAt IS NULL clause guarantees a single-use guarantee even when two
    // requests race with the same code.
    @Modifying
    @Query("UPDATE BackupCodeEntity b SET b.usedAt = :usedAt "
            + "WHERE b.backupCodeId = :backupCodeId AND b.usedAt IS NULL")
    int markAsUsedIfUnused(@Param("backupCodeId") String backupCodeId,
                           @Param("usedAt") Instant usedAt);
}
