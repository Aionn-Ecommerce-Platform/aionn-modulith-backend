package com.aionn.promotion.infrastructure.persistence.mapper;

import com.aionn.promotion.domain.model.FlashSaleRegistration;
import com.aionn.promotion.domain.valueobject.FlashSaleRegistrationStatus;
import com.aionn.promotion.infrastructure.persistence.entity.FlashSaleRegistrationEntity;
import com.aionn.sharedkernel.domain.vo.Money;
import org.springframework.stereotype.Component;

@Component
public class FlashSaleRegistrationDomainMapper {

    public FlashSaleRegistration toDomain(FlashSaleRegistrationEntity e) {
        return new FlashSaleRegistration(
                e.getRegistrationId(),
                e.getCampaignId(),
                e.getMerchantId(),
                e.getProductId(),
                e.getSkuId(),
                Money.of(e.getSalePrice(), e.getCurrency()),
                e.getSaleStock(),
                e.getSoldCount(),
                FlashSaleRegistrationStatus.valueOf(e.getStatus()),
                e.getRejectReason(),
                e.getSubmittedAt(),
                e.getDecidedAt(),
                e.getDecidedBy(),
                e.getUpdatedAt());
    }

    public FlashSaleRegistrationEntity toEntity(FlashSaleRegistration r, FlashSaleRegistrationEntity existing) {
        FlashSaleRegistrationEntity entity = existing != null ? existing
                : FlashSaleRegistrationEntity.builder()
                        .registrationId(r.getRegistrationId())
                        .campaignId(r.getCampaignId())
                        .merchantId(r.getMerchantId())
                        .productId(r.getProductId())
                        .skuId(r.getSkuId())
                        .build();
        entity.setSalePrice(r.getSalePrice().amount());
        entity.setCurrency(r.getSalePrice().currency());
        entity.setSaleStock(r.getSaleStock());
        entity.setSoldCount(r.getSoldCount());
        entity.setStatus(r.getStatus().name());
        entity.setRejectReason(r.getRejectReason());
        entity.setDecidedAt(r.getDecidedAt());
        entity.setDecidedBy(r.getDecidedBy());
        return entity;
    }
}
