package com.aionn.catalog.infrastructure.persistence.mapper;

import com.aionn.catalog.domain.model.UserBrowsingHistory;
import com.aionn.catalog.infrastructure.persistence.entity.UserBrowsingHistoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface UserBrowsingHistoryDomainMapper {

    @Mapping(target = "categoryIds", expression = "java(copyOf(entity.getCategoryIds()))")
    @Mapping(target = "brandIds", expression = "java(copyOf(entity.getBrandIds()))")
    @Mapping(target = "recentSearches", expression = "java(copyOf(entity.getRecentSearches()))")
    UserBrowsingHistory toDomain(UserBrowsingHistoryEntity entity);

    @Mapping(target = "updatedAt", ignore = true)
    UserBrowsingHistoryEntity toEntity(UserBrowsingHistory domain);

    default List<String> copyOf(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
