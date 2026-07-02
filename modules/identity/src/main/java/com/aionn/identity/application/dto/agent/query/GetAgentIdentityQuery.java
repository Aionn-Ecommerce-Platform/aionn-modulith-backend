package com.aionn.identity.application.dto.agent.query;

import com.aionn.sharedkernel.application.query.Query;

public record GetAgentIdentityQuery(String userId, String agentId) implements Query {
}

