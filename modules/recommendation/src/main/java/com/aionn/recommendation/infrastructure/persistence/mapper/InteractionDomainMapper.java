package com.aionn.recommendation.infrastructure.persistence.mapper;

import com.aionn.recommendation.domain.model.UserInteraction;
import com.aionn.recommendation.domain.valueobject.InteractionType;
import com.aionn.recommendation.infrastructure.persistence.entity.InteractionEntity;
import org.springframework.stereotype.Component;

@Component
public class InteractionDomainMapper {

    public UserInteraction toDomain(InteractionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new UserInteraction(
                entity.getInteractionId(),
                entity.getUserId(),
                entity.getProductId(),
                InteractionType.valueOf(entity.getInteractionType()),
                entity.getWeight(),
                entity.getOccurredAt());
    }

    public InteractionEntity toEntity(UserInteraction domain, java.time.Instant createdAt) {
        if (domain == null) {
            return null;
        }
        return InteractionEntity.builder()
                .interactionId(domain.getInteractionId())
                .userId(domain.getUserId())
                .productId(domain.getProductId())
                .interactionType(domain.getType().name())
                .weight(domain.getWeight())
                .occurredAt(domain.getOccurredAt())
                .createdAt(createdAt)
                .build();
    }
}
