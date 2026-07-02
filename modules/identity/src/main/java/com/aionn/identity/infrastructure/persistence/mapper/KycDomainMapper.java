package com.aionn.identity.infrastructure.persistence.mapper;

import com.aionn.identity.domain.model.KycProfile;
import com.aionn.identity.domain.valueobject.KycStatus;
import com.aionn.identity.infrastructure.persistence.entity.KycProfileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = { KycStatus.class })
public interface KycDomainMapper {

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", expression = "java(domain.getStatus().name())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "expiredAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    KycProfileEntity toEntity(KycProfile domain);

    @Mapping(target = "status", expression = "java(KycStatus.valueOf(entity.getStatus()))")
    @Mapping(target = "userId", source = "user.userId")
    KycProfile toDomain(KycProfileEntity entity);
}
