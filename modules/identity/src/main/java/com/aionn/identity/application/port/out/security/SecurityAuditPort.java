package com.aionn.identity.application.port.out.security;

import com.aionn.identity.domain.model.SecurityAudit;
import com.aionn.identity.domain.valueobject.SecurityAuditEventType;

import java.util.List;

public interface SecurityAuditPort {

    void saveAuditLog(String userId, SecurityAuditEventType event, String ipAddress);

    List<SecurityAudit> getAuditLogs(String userId);
}
