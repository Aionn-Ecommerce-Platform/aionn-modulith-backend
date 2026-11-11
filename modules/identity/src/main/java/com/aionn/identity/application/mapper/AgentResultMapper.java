package com.aionn.identity.application.mapper;

import com.aionn.identity.application.dto.agent.result.AgentIdentityResult;
import com.aionn.identity.application.dto.agent.result.AgentAuditLogResult;
import com.aionn.identity.domain.model.AgentIdentity;
import com.aionn.identity.domain.model.SecurityAudit;
import com.aionn.identity.domain.valueobject.AgentStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface AgentResultMapper {

    @Mapping(source = "id", target = "agentId")
    @Mapping(source = "status", target = "status", qualifiedByName = "statusToString")
    @Mapping(source = "expiresAt", target = "expiryAt")
    @Mapping(target = "apiKey", ignore = true)
    AgentIdentityResult toIdentityResult(AgentIdentity agentIdentity);

    default AgentIdentityResult toCreatedIdentityResult(AgentIdentity agentIdentity, String apiKey) {
        AgentIdentityResult base = toIdentityResult(agentIdentity);
        return new AgentIdentityResult(base.agentId(), apiKey, base.permissions(), base.status(),
                base.expiryAt(), base.createdAt());
    }

    @Mapping(source = "id", target = "auditId")
    AgentAuditLogResult toAuditLogResult(SecurityAudit securityAudit);

    @Named("statusToString")
    default String statusToString(AgentStatus status) {
        return status != null ? status.name() : null;
    }
}
