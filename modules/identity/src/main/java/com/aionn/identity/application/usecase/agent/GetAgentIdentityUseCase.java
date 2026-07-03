package com.aionn.identity.application.usecase.agent;

import com.aionn.identity.application.dto.agent.query.GetAgentIdentityQuery;
import com.aionn.identity.application.dto.agent.result.AgentIdentityResult;
import com.aionn.identity.application.mapper.AgentResultMapper;
import com.aionn.identity.application.port.in.agent.GetAgentIdentityQueryPort;
import com.aionn.identity.application.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetAgentIdentityUseCase implements GetAgentIdentityQueryPort {

    private final AgentService agentService;
    private final AgentResultMapper agentResultMapper;

    @Override
    @Transactional(readOnly = true)
    public AgentIdentityResult execute(GetAgentIdentityQuery query) {
        var entity = agentService.get(query.userId(), query.agentId());
        return agentResultMapper.toIdentityResult(entity);
    }
}

