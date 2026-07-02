package com.aionn.identity.infrastructure.persistence.mapper;

import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IdentityUserMapper {

    IdentityUser toDomain(UserEntity entity);

    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "mfaEnabled", ignore = true)
    @Mapping(target = "mfaSecret", ignore = true)
    @Mapping(target = "failedLoginAttempts", ignore = true)
    @Mapping(target = "version", ignore = true)
    UserEntity toEntity(IdentityUser user);
}
