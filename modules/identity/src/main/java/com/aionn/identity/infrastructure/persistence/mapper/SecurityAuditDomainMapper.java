package com.aionn.identity.infrastructure.persistence.mapper;

import com.aionn.identity.domain.model.SecurityAudit;
import com.aionn.identity.infrastructure.persistence.entity.SecurityAuditEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring")
public interface SecurityAuditDomainMapper {

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "auditId", source = "id")
    SecurityAuditEntity toEntity(SecurityAudit domain);

    @Mapping(target = "id", source = "auditId")
    @Mapping(target = "userId", source = "user.userId")
    SecurityAudit toDomain(SecurityAuditEntity entity);
}

