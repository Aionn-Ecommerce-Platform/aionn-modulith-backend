package com.aionn.identity.application.port.in.agent;

import com.aionn.identity.application.dto.agent.query.GetAgentIdentityQuery;
import com.aionn.identity.application.dto.agent.result.AgentIdentityResult;

public interface GetAgentIdentityQueryPort {
    AgentIdentityResult execute(GetAgentIdentityQuery query);
}

