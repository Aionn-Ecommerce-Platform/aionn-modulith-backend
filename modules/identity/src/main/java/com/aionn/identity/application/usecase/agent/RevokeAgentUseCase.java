package com.aionn.identity.application.usecase.agent;

import com.aionn.identity.application.dto.agent.command.RevokeAgentCommand;
import com.aionn.identity.application.port.in.agent.RevokeAgentInputPort;
import com.aionn.identity.application.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RevokeAgentUseCase implements RevokeAgentInputPort {

    private final AgentService agentService;

    @Override
    @Transactional
    public void execute(RevokeAgentCommand command) {
        agentService.revoke(command.ownerUserId(), command.agentId());
    }
}

