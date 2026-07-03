package com.aionn.identity.application.service;

import com.aionn.identity.application.port.out.security.SecurityAuditPort;
import com.aionn.identity.domain.model.SecurityAudit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SecurityAuditService {

    private final SecurityAuditPort securityAuditPort;

    public List<SecurityAudit> getAuditLogs(String userId) {
        log.debug("Retrieving audit logs for user: {}", userId);
        return securityAuditPort.getAuditLogs(userId);
    }
}
