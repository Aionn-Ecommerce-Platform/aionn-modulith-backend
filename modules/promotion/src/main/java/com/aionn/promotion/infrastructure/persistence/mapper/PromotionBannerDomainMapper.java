package com.aionn.promotion.infrastructure.persistence.mapper;

import com.aionn.promotion.domain.model.PromotionBanner;
import com.aionn.promotion.infrastructure.persistence.entity.PromotionBannerEntity;
import org.springframework.stereotype.Component;

@Component
public class PromotionBannerDomainMapper {

    public PromotionBanner toDomain(PromotionBannerEntity e) {
        return new PromotionBanner(
                e.getBannerId(),
                e.getTitle(),
                e.getImageUrl(),
                e.getLinkUrl(),
                e.getDisplayOrder(),
                e.isActive(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    public PromotionBannerEntity toEntity(PromotionBanner b, PromotionBannerEntity existing) {
        PromotionBannerEntity entity = existing != null ? existing
                : PromotionBannerEntity.builder()
                        .bannerId(b.getBannerId())
                        .build();
        entity.setTitle(b.getTitle());
        entity.setImageUrl(b.getImageUrl());
        entity.setLinkUrl(b.getLinkUrl());
        entity.setDisplayOrder(b.getDisplayOrder());
        entity.setActive(b.isActive());
        return entity;
    }
}
