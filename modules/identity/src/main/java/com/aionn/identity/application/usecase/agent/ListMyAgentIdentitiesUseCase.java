package com.aionn.identity.application.usecase.agent;

import com.aionn.identity.application.dto.agent.result.AgentIdentityResult;
import com.aionn.identity.application.mapper.AgentResultMapper;
import com.aionn.identity.application.port.in.agent.ListMyAgentIdentitiesQueryPort;
import com.aionn.identity.application.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListMyAgentIdentitiesUseCase implements ListMyAgentIdentitiesQueryPort {

    private final AgentService agentService;
    private final AgentResultMapper agentResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AgentIdentityResult> execute(String userId) {
        return agentService.listMy(userId).stream()
                .map(agentResultMapper::toIdentityResult)
                .toList();
    }
}

