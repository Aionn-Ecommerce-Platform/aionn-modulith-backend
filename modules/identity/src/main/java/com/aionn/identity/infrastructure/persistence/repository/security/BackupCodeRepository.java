package com.aionn.identity.infrastructure.persistence.repository.security;

import com.aionn.identity.infrastructure.persistence.entity.BackupCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BackupCodeRepository extends JpaRepository<BackupCodeEntity, String> {

    List<BackupCodeEntity> findByUser_UserIdAndUsedAtIsNullOrderByGeneratedAtDesc(String userId);

    void deleteByUser_UserId(String userId);
}
