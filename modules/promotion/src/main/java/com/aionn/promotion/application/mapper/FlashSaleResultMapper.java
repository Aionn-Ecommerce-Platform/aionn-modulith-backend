package com.aionn.promotion.application.mapper;

import com.aionn.promotion.application.dto.flashsale.result.FlashSaleRegistrationResult;
import com.aionn.promotion.domain.model.FlashSaleRegistration;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FlashSaleResultMapper {

    public FlashSaleRegistrationResult toResult(FlashSaleRegistration r) {
        return new FlashSaleRegistrationResult(
                r.getRegistrationId(),
                r.getCampaignId(),
                r.getMerchantId(),
                r.getProductId(),
                r.getSkuId(),
                r.getSalePrice() == null ? null : r.getSalePrice().amount(),
                r.getSalePrice() == null ? null : r.getSalePrice().currency(),
                r.getSaleStock(),
                r.getSoldCount(),
                r.getStatus().name(),
                r.getRejectReason(),
                r.getSubmittedAt(),
                r.getDecidedAt(),
                r.getDecidedBy(),
                r.getUpdatedAt());
    }

    public List<FlashSaleRegistrationResult> toResults(List<FlashSaleRegistration> registrations) {
        return registrations.stream().map(this::toResult).toList();
    }
}
