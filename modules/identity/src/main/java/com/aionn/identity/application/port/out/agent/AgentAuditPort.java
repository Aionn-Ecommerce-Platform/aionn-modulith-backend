package com.aionn.identity.application.port.out.agent;

import com.aionn.identity.domain.model.SecurityAudit;

import java.util.List;

public interface AgentAuditPort {

    SecurityAudit save(SecurityAudit securityAudit);

    List<SecurityAudit> findByAgentId(String agentId, int limit);
}
