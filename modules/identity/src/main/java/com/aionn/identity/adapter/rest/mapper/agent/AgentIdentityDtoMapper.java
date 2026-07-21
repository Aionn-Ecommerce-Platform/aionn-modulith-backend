package com.aionn.identity.adapter.rest.mapper.agent;

import com.aionn.identity.adapter.rest.dto.agent.request.CreateAgentIdentityRequest;
import com.aionn.identity.adapter.rest.dto.agent.request.UpdateAgentPermissionsRequest;
import com.aionn.identity.adapter.rest.dto.agent.response.AgentAuditLogResponse;
import com.aionn.identity.adapter.rest.dto.agent.response.AgentIdentityResponse;
import com.aionn.identity.application.dto.agent.result.AgentIdentityResult;
import com.aionn.identity.application.dto.agent.command.CreateAgentIdentityCommand;
import com.aionn.identity.application.dto.agent.query.GetAgentIdentityQuery;
import com.aionn.identity.application.dto.agent.command.RevokeAgentCommand;
import com.aionn.identity.application.dto.agent.command.SuspendAgentCommand;
import com.aionn.identity.application.dto.agent.command.UpdateAgentPermissionsCommand;
import com.aionn.identity.application.dto.agent.query.GetAgentAuditLogsQuery;
import com.aionn.identity.application.dto.agent.result.AgentAuditLogResult;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AgentIdentityDtoMapper {

    @Mapping(target = "ownerUserId", source = "userId")
    CreateAgentIdentityCommand toCreateCommand(String userId, CreateAgentIdentityRequest request);

    @Mapping(target = "ownerUserId", source = "userId")
    UpdateAgentPermissionsCommand toUpdatePermissionsCommand(String userId, String agentId,
            UpdateAgentPermissionsRequest request);

    @Mapping(target = "ownerUserId", source = "userId")
    SuspendAgentCommand toSuspendCommand(String userId, String agentId);

    @Mapping(target = "ownerUserId", source = "userId")
    GetAgentAuditLogsQuery toGetAuditLogsQuery(String userId, String agentId);

    GetAgentIdentityQuery toGetAgentQuery(String userId, String agentId);

    @Mapping(target = "ownerUserId", source = "userId")
    RevokeAgentCommand toRevokeCommand(String userId, String agentId);

    AgentIdentityResponse toResponse(AgentIdentityResult entity);

    List<AgentIdentityResponse> toResponses(List<AgentIdentityResult> entities);

    List<AgentAuditLogResponse> toAuditLogResponses(List<AgentAuditLogResult> audits);

    AgentAuditLogResponse toAuditLogResponse(AgentAuditLogResult audit);
}
