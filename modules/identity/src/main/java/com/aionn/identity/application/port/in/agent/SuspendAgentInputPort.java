package com.aionn.identity.application.port.in.agent;

import com.aionn.identity.application.dto.agent.result.AgentIdentityResult;
import com.aionn.identity.application.dto.agent.command.SuspendAgentCommand;

public interface SuspendAgentInputPort {
    AgentIdentityResult execute(SuspendAgentCommand command);
}

