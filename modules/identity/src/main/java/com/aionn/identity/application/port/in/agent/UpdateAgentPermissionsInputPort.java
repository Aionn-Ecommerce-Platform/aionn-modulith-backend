package com.aionn.identity.application.port.in.agent;

import com.aionn.identity.application.dto.agent.result.AgentIdentityResult;
import com.aionn.identity.application.dto.agent.command.UpdateAgentPermissionsCommand;

public interface UpdateAgentPermissionsInputPort {
    AgentIdentityResult execute(UpdateAgentPermissionsCommand command);
}
