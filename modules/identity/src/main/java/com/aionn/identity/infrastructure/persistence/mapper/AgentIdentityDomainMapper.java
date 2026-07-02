package com.aionn.identity.infrastructure.persistence.mapper;

import com.aionn.identity.domain.model.AgentIdentity;
import com.aionn.identity.domain.valueobject.AgentStatus;
import com.aionn.identity.infrastructure.persistence.entity.AgentIdentityEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", imports = { AgentStatus.class })
public interface AgentIdentityDomainMapper {

    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "status", expression = "java(domain.getStatus().name())")
    @Mapping(target = "agentId", source = "id")
    @Mapping(target = "expiryAt", source = "expiresAt")
    AgentIdentityEntity toEntity(AgentIdentity domain);

    @Mapping(target = "status", expression = "java(AgentStatus.valueOf(entity.getStatus()))")
    @Mapping(target = "id", source = "agentId")
    @Mapping(target = "ownerId", source = "owner.userId")
    @Mapping(target = "name", source = "agentId")
    @Mapping(target = "expiresAt", source = "expiryAt")
    @Mapping(target = "updatedAt", ignore = true)
    AgentIdentity toDomain(AgentIdentityEntity entity);
}

