package com.aionn.identity.application.usecase.agent;

import com.aionn.identity.application.dto.agent.command.SuspendAgentCommand;
import com.aionn.identity.application.dto.agent.result.AgentIdentityResult;
import com.aionn.identity.application.mapper.AgentResultMapper;
import com.aionn.identity.application.port.in.agent.SuspendAgentInputPort;
import com.aionn.identity.application.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SuspendAgentUseCase implements SuspendAgentInputPort {

    private final AgentService agentService;
    private final AgentResultMapper agentResultMapper;

    @Override
    @Transactional
    public AgentIdentityResult execute(SuspendAgentCommand command) {
        var result = agentService.suspend(command.ownerUserId(), command.agentId());
        return agentResultMapper.toIdentityResult(result);
    }
}

