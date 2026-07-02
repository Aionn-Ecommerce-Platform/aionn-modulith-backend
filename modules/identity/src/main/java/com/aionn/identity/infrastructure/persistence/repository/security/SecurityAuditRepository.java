package com.aionn.identity.infrastructure.persistence.repository.security;

import com.aionn.identity.infrastructure.persistence.entity.SecurityAuditEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecurityAuditRepository extends JpaRepository<SecurityAuditEntity, String> {

    List<SecurityAuditEntity> findTop100ByUser_UserIdOrderByTimestampDesc(String userId);
    List<SecurityAuditEntity> findByDescriptionContainingOrderByTimestampDesc(String descriptionPattern,
            Pageable pageable);
}

