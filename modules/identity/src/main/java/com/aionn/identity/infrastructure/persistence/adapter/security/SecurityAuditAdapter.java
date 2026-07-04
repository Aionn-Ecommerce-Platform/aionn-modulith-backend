package com.aionn.identity.infrastructure.persistence.adapter.security;

import com.aionn.identity.application.port.out.security.SecurityAuditPort;
import com.aionn.identity.domain.valueobject.SecurityAuditEventType;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.SecurityAudit;
import com.aionn.identity.infrastructure.persistence.entity.SecurityAuditEntity;
import com.aionn.identity.infrastructure.persistence.mapper.SecurityAuditDomainMapper;
import com.aionn.identity.infrastructure.persistence.repository.security.SecurityAuditRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SecurityAuditAdapter implements SecurityAuditPort {

    private final UserRepository userRepository;
    private final SecurityAuditRepository securityAuditRepository;
    private final SecurityAuditDomainMapper securityAuditDomainMapper;

    @Override
    public void saveAuditLog(String userId, SecurityAuditEventType event, String ipAddress) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));

        securityAuditRepository.save(SecurityAuditEntity.builder()
                .auditId(IdGenerator.ulid())
                .user(user)
                .eventType(event.eventType())
                .description(event.description())
                .ipAddress(ipAddress)
                .deviceId(null)
                .build());
    }

    @Override
    public List<SecurityAudit> getAuditLogs(String userId) {
        return securityAuditRepository.findTop100ByUser_UserIdOrderByTimestampDesc(userId).stream()
                .map(securityAuditDomainMapper::toDomain)
                .toList();
    }
}
