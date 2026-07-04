package com.aionn.identity.infrastructure.persistence.mapper;

import com.aionn.identity.domain.model.AuthSession;
import com.aionn.identity.domain.valueobject.AuthSessionStatus;
import com.aionn.identity.infrastructure.persistence.entity.AuthSessionEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AuthSessionDomainMapper {

    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "status", source = "entity.status")
    @Mapping(target = "createdAt", source = "entity.createdAt")
    AuthSession toDomain(AuthSessionEntity entity);

    @Mapping(target = "user", source = "userEntity")
    @Mapping(target = "status", source = "domain.status")
    @Mapping(target = "createdAt", source = "domain.createdAt")
    AuthSessionEntity toEntity(AuthSession domain, UserEntity userEntity);

    // Update-in-place mapper preserves the managed entity's version and
    // createdAt so refresh/revoke/logout paths remain optimistic-lock safe.
    @Mapping(target = "sessionId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", source = "domain.status")
    void updateEntity(@MappingTarget AuthSessionEntity entity, AuthSession domain);

    default AuthSessionStatus mapStatus(String value) {
        return value == null ? null : AuthSessionStatus.valueOf(value);
    }

    default String mapStatus(AuthSessionStatus value) {
        return value == null ? null : value.name();
    }
}

