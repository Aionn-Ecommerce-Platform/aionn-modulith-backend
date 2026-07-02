package com.aionn.identity.infrastructure.persistence.mapper;

import com.aionn.identity.domain.model.AuthSession;
import com.aionn.identity.domain.valueobject.AuthSessionStatus;
import com.aionn.identity.infrastructure.persistence.entity.AuthSessionEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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

    default AuthSessionStatus mapStatus(String value) {
        return value == null ? null : AuthSessionStatus.valueOf(value);
    }

    default String mapStatus(AuthSessionStatus value) {
        return value == null ? null : value.name();
    }
}

