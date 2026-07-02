package com.aionn.identity.application.port.in.agent;

import com.aionn.identity.application.dto.agent.query.GetAgentAuditLogsQuery;
import com.aionn.identity.application.dto.agent.result.AgentAuditLogResult;

import java.util.List;

public interface GetAgentAuditLogsQueryPort {
    List<AgentAuditLogResult> execute(GetAgentAuditLogsQuery query);
}
