package com.aionn.identity.application.dto.agent.query;

import com.aionn.sharedkernel.application.query.Query;

public record GetAgentAuditLogsQuery(
        String ownerUserId,
        String agentId) implements Query {
}



