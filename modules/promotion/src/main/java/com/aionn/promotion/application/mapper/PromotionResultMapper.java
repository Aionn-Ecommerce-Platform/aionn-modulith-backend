package com.aionn.promotion.application.mapper;

import com.aionn.promotion.application.dto.banner.result.PromotionBannerResult;
import com.aionn.promotion.application.dto.campaign.result.CampaignResult;
import com.aionn.promotion.application.dto.voucher.result.UserVoucherResult;
import com.aionn.promotion.application.dto.voucher.result.VoucherResult;
import com.aionn.promotion.domain.model.PromotionBanner;
import com.aionn.promotion.domain.model.PromotionCampaign;
import com.aionn.promotion.domain.model.UserVoucher;
import com.aionn.promotion.domain.model.Voucher;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PromotionResultMapper {

    @Mapping(target = "budget", source = "budget.amount")
    @Mapping(target = "budgetRemaining", source = "budgetRemaining.amount")
    @Mapping(target = "currency", source = "budget.currency")
    @Mapping(target = "minOrderValue", source = "condition.minOrderValue")
    @Mapping(target = "applicableCategoryIds", source = "condition.applicableCategoryIds")
    @Mapping(target = "maxClaimsPerUser", source = "condition.maxClaimsPerUser")
    @Mapping(target = "maxUsesPerVoucher", source = "condition.maxUsesPerVoucher")
    CampaignResult toResult(PromotionCampaign c);

    @Mapping(target = "discountAmount", source = "discountAmount.amount")
    @Mapping(target = "currency", source = "discountAmount.currency")
    VoucherResult toResult(Voucher v);

    @Mapping(target = "appliedAmount", source = "appliedAmount.amount")
    @Mapping(target = "currency", source = "appliedAmount.currency")
    UserVoucherResult toResult(UserVoucher u);

    PromotionBannerResult toResult(PromotionBanner b);
}
