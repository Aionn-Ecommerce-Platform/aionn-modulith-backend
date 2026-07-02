package com.aionn.identity.application.usecase.agent;

import com.aionn.identity.application.dto.agent.query.GetAgentAuditLogsQuery;
import com.aionn.identity.application.dto.agent.result.AgentAuditLogResult;
import com.aionn.identity.application.mapper.AgentResultMapper;
import com.aionn.identity.application.port.in.agent.GetAgentAuditLogsQueryPort;
import com.aionn.identity.application.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetAgentAuditLogsUseCase implements GetAgentAuditLogsQueryPort {

    private final AgentService agentService;
    private final AgentResultMapper agentResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AgentAuditLogResult> execute(GetAgentAuditLogsQuery query) {
        return agentService.getAgentAuditLogs(query.ownerUserId(), query.agentId()).stream()
                .map(agentResultMapper::toAuditLogResult)
                .toList();
    }
}

