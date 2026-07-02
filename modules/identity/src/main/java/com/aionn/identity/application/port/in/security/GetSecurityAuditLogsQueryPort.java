package com.aionn.identity.application.port.in.security;

import com.aionn.identity.application.dto.security.result.SecurityAuditLogResult;
import java.util.List;

public interface GetSecurityAuditLogsQueryPort {
    List<SecurityAuditLogResult> execute(String userId);
}



