package com.aionn.promotion.application.mapper;

import com.aionn.promotion.application.dto.campaign.result.CampaignResult;
import com.aionn.promotion.domain.model.PromotionCampaign;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CampaignResultMapper {

    @Mapping(target = "type", expression = "java(campaign.getType().name())")
    @Mapping(target = "budget", expression = "java(campaign.getBudget().amount())")
    @Mapping(target = "budgetRemaining", expression = "java(campaign.getBudgetRemaining().amount())")
    @Mapping(target = "currency", expression = "java(campaign.getBudget().currency())")
    @Mapping(target = "status", expression = "java(campaign.getStatus().name())")
    @Mapping(target = "minOrderValue", expression = "java(campaign.getCondition().minOrderValue())")
    @Mapping(target = "applicableCategoryIds", expression = "java(campaign.getCondition().applicableCategoryIds())")
    @Mapping(target = "maxClaimsPerUser", expression = "java(campaign.getCondition().maxClaimsPerUser())")
    @Mapping(target = "maxUsesPerVoucher", expression = "java(campaign.getCondition().maxUsesPerVoucher())")
    CampaignResult toResult(PromotionCampaign campaign);
}
