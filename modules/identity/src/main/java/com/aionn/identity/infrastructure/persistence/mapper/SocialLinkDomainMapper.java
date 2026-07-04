package com.aionn.identity.infrastructure.persistence.mapper;

import com.aionn.identity.domain.model.SocialLink;
import com.aionn.identity.infrastructure.persistence.entity.SocialAccountEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SocialLinkDomainMapper {

    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "provider", expression = "java(AuthProvider.valueOf(entity.getProvider()))")
    SocialLink toDomain(SocialAccountEntity entity);

    @Mapping(target = "user", source = "userEntity")
    @Mapping(target = "provider", expression = "java(domain.provider().name())")
    @Mapping(target = "createdAt", source = "domain.createdAt")
    SocialAccountEntity toEntity(SocialLink domain, UserEntity userEntity);
}
