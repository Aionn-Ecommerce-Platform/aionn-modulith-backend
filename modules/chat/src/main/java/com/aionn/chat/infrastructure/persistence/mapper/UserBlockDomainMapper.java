package com.aionn.chat.infrastructure.persistence.mapper;

import com.aionn.chat.domain.model.UserBlock;
import com.aionn.chat.infrastructure.persistence.entity.UserBlockEntity;
import org.springframework.stereotype.Component;

@Component
public class UserBlockDomainMapper {

    public UserBlock toDomain(UserBlockEntity e) {
        return new UserBlock(e.getBlockId(), e.getBlockerId(), e.getBlockedId(), e.getReason(),
                e.isActive(), e.getCreatedAt(), e.getUpdatedAt());
    }

    public UserBlockEntity toEntity(UserBlock b, UserBlockEntity existing) {
        UserBlockEntity entity = existing != null ? existing
                : UserBlockEntity.builder()
                        .blockId(b.getBlockId())
                        .blockerId(b.getBlockerId())
                        .blockedId(b.getBlockedId())
                        .build();
        entity.setReason(b.getReason());
        entity.setActive(b.isActive());
        return entity;
    }
}

