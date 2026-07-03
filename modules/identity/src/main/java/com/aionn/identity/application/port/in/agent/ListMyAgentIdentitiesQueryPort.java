package com.aionn.identity.application.port.in.agent;

import com.aionn.identity.application.dto.agent.result.AgentIdentityResult;
import java.util.List;

public interface ListMyAgentIdentitiesQueryPort {
    List<AgentIdentityResult> execute(String userId);
}
