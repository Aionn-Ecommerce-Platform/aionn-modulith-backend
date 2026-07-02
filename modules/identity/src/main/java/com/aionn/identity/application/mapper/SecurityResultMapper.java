package com.aionn.identity.application.mapper;

import com.aionn.identity.application.dto.security.result.SecurityAuditLogResult;
import com.aionn.identity.domain.model.SecurityAudit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SecurityResultMapper {

    @Mapping(target = "auditId", source = "id")
    SecurityAuditLogResult toAuditLogResult(SecurityAudit audit);
}
