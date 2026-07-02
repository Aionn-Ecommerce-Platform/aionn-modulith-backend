package com.aionn.identity.application.port.in.agent;

import com.aionn.identity.application.dto.agent.command.CreateAgentIdentityCommand;
import com.aionn.identity.application.dto.agent.result.AgentIdentityResult;

public interface CreateAgentIdentityInputPort {
    AgentIdentityResult execute(CreateAgentIdentityCommand command);
}

