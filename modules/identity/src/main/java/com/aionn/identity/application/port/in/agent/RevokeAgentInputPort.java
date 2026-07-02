package com.aionn.identity.application.port.in.agent;

import com.aionn.identity.application.dto.agent.command.RevokeAgentCommand;

public interface RevokeAgentInputPort {
    void execute(RevokeAgentCommand command);
}


