package com.aionn.identity.application.usecase.agent;

import com.aionn.identity.application.dto.agent.command.CreateAgentIdentityCommand;
import com.aionn.identity.application.dto.agent.result.AgentIdentityResult;
import com.aionn.identity.application.mapper.AgentResultMapper;
import com.aionn.identity.application.port.in.agent.CreateAgentIdentityInputPort;
import com.aionn.identity.application.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateAgentIdentityUseCase implements CreateAgentIdentityInputPort {

    private final AgentService agentService;
    private final AgentResultMapper agentResultMapper;

    @Override
    @Transactional
    public AgentIdentityResult execute(CreateAgentIdentityCommand command) {
        var result = agentService.create(command.ownerUserId());
        return agentResultMapper.toIdentityResult(result);
    }
}

