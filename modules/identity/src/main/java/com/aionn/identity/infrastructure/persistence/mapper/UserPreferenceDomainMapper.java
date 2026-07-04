package com.aionn.identity.infrastructure.persistence.mapper;

import com.aionn.identity.application.dto.preference.result.UserPreferenceResult;
import com.aionn.identity.infrastructure.persistence.entity.UserPreferenceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserPreferenceDomainMapper {

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "isNew", ignore = true)
    UserPreferenceEntity toEntity(UserPreferenceResult result);

    UserPreferenceResult toResult(UserPreferenceEntity entity);
}

