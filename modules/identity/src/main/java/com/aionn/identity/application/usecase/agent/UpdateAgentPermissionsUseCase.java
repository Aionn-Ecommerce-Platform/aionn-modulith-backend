package com.aionn.identity.application.usecase.agent;

import com.aionn.identity.application.dto.agent.command.UpdateAgentPermissionsCommand;
import com.aionn.identity.application.dto.agent.result.AgentIdentityResult;
import com.aionn.identity.application.mapper.AgentResultMapper;
import com.aionn.identity.application.port.in.agent.UpdateAgentPermissionsInputPort;
import com.aionn.identity.application.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateAgentPermissionsUseCase implements UpdateAgentPermissionsInputPort {

    private final AgentService agentService;
    private final AgentResultMapper agentResultMapper;

    @Override
    @Transactional
    public AgentIdentityResult execute(UpdateAgentPermissionsCommand command) {
        var result = agentService.updatePermissions(command.ownerUserId(), command.agentId(),
                command.permissionsJson());
        return agentResultMapper.toIdentityResult(result);
    }
}

