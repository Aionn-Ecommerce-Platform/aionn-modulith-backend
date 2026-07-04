package com.aionn.identity.infrastructure.persistence.repository.security;

import com.aionn.identity.infrastructure.persistence.entity.SecurityAuditEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecurityAuditRepository extends JpaRepository<SecurityAuditEntity, String> {

    List<SecurityAuditEntity> findTop100ByUser_UserIdOrderByTimestampDesc(String userId);

    // Agent audit rows are written with deviceId = agentId (see AgentService).
    // Query the exact column instead of doing LIKE '%agentId%' on description so
    // the index can be used and unrelated audit entries never leak through.
    List<SecurityAuditEntity> findByDeviceIdOrderByTimestampDesc(String deviceId, Pageable pageable);
}
