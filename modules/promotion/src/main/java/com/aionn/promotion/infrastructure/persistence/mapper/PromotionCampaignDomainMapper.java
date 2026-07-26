package com.aionn.promotion.infrastructure.persistence.mapper;

import com.aionn.promotion.domain.model.PromotionCampaign;
import com.aionn.promotion.domain.valueobject.CampaignStatus;
import com.aionn.promotion.domain.valueobject.CampaignType;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.promotion.domain.valueobject.PromotionCondition;
import com.aionn.promotion.infrastructure.persistence.entity.PromotionCampaignEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromotionCampaignDomainMapper {

    public PromotionCampaign toDomain(PromotionCampaignEntity e) {
        PromotionCondition condition = new PromotionCondition(
                e.getMinOrderValue(),
                e.getApplicableCategories() == null ? List.of() : e.getApplicableCategories(),
                e.getMaxClaimsPerUser(),
                e.getMaxUsesPerVoucher());
        return new PromotionCampaign(
                e.getCampaignId(),
                e.getName(),
                CampaignType.valueOf(e.getType()),
                Money.of(e.getBudget(), e.getCurrency()),
                Money.of(e.getBudgetRemaining(), e.getCurrency()),
                e.getStartDate(),
                e.getEndDate(),
                e.getCreatedBy(),
                CampaignStatus.valueOf(e.getStatus()),
                condition,
                e.getCreatedAt(),
                e.getUpdatedAt());
    }

    public PromotionCampaignEntity toEntity(PromotionCampaign c, PromotionCampaignEntity existing) {
        PromotionCampaignEntity entity = existing != null ? existing
                : PromotionCampaignEntity.builder()
                        .campaignId(c.getCampaignId())
                        .name(c.getName())
                        .type(c.getType().name())
                        .budget(c.getBudget().amount())
                        .currency(c.getBudget().currency())
                        .startDate(c.getStartDate())
                        .endDate(c.getEndDate())
                        .createdBy(c.getCreatedBy())
                        .build();
        entity.setBudgetRemaining(c.getBudgetRemaining().amount());
        entity.setStatus(c.getStatus().name());
        PromotionCondition condition = c.getCondition();
        entity.setMinOrderValue(condition.minOrderValue());
        entity.setApplicableCategories(condition.applicableCategoryIds());
        entity.setMaxClaimsPerUser(condition.maxClaimsPerUser());
        entity.setMaxUsesPerVoucher(condition.maxUsesPerVoucher());
        return entity;
    }
}

