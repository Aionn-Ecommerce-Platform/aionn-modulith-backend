package com.aionn.notification.infrastructure.persistence.mapper;

import com.aionn.notification.domain.model.DeviceToken;
import com.aionn.notification.infrastructure.persistence.entity.DeviceTokenEntity;
import org.springframework.stereotype.Component;

@Component
public class DeviceTokenDomainMapper {

    public DeviceToken toDomain(DeviceTokenEntity e) {
        return new DeviceToken(e.getTokenId(), e.getUserId(), e.getDeviceToken(), e.getOs(),
                e.isActive(), e.getRegisteredAt(), e.getUpdatedAt());
    }

    public DeviceTokenEntity toEntity(DeviceToken t, DeviceTokenEntity existing) {
        DeviceTokenEntity entity = existing != null ? existing
                : DeviceTokenEntity.builder()
                        .tokenId(t.getTokenId())
                        .userId(t.getUserId())
                        .deviceToken(t.getDeviceToken())
                        .os(t.getOs())
                        .build();
        entity.setActive(t.isActive());
        return entity;
    }
}

