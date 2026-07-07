package com.aionn.catalog.infrastructure.persistence.mapper;

import com.aionn.catalog.domain.model.UserBrowsingHistory;
import com.aionn.catalog.infrastructure.persistence.entity.UserBrowsingHistoryEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class UserBrowsingHistoryDomainMapper {

    public UserBrowsingHistory toDomain(UserBrowsingHistoryEntity entity) {
        if (entity == null) {
            return null;
        }
        return new UserBrowsingHistory(
                entity.getUserId(),
                new ArrayList<>(entity.getCategoryIds()),
                new ArrayList<>(entity.getBrandIds()));
    }

    public UserBrowsingHistoryEntity toEntity(UserBrowsingHistory domain) {
        if (domain == null) {
            return null;
        }
        return UserBrowsingHistoryEntity.builder()
                .userId(domain.getUserId())
                .categoryIds(new ArrayList<>(domain.getCategoryIds()))
                .brandIds(new ArrayList<>(domain.getBrandIds()))
                .build();
    }
}
