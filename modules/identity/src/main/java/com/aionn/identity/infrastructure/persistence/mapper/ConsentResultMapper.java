package com.aionn.identity.infrastructure.persistence.mapper;

import com.aionn.identity.application.dto.consent.result.ConsentResult;
import com.aionn.identity.infrastructure.persistence.entity.UserConsentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ConsentResultMapper {

    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "agreed", expression = "java(entity.getRevokedAt() == null)")
    ConsentResult toResult(UserConsentEntity entity);

    List<ConsentResult> toResults(List<UserConsentEntity> entities);
}
