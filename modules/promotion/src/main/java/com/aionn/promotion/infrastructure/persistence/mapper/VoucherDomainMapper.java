package com.aionn.promotion.infrastructure.persistence.mapper;

import com.aionn.promotion.domain.model.Voucher;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.promotion.infrastructure.persistence.entity.VoucherEntity;
import org.springframework.stereotype.Component;

@Component
public class VoucherDomainMapper {

    public Voucher toDomain(VoucherEntity e) {
        return new Voucher(
                e.getVoucherCode(),
                e.getCampaignId(),
                e.getScope(),
                e.getMerchantId(),
                Money.of(e.getDiscountAmount(), e.getCurrency()),
                e.getUsageLimit(),
                e.getUsedCount(),
                e.getReservedCount(),
                e.getValidFrom(),
                e.getValidUntil(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }

    public VoucherEntity toEntity(Voucher v, VoucherEntity existing) {
        VoucherEntity entity = existing != null ? existing
                : VoucherEntity.builder()
                        .voucherCode(v.getVoucherCode())
                        .campaignId(v.getCampaignId())
                        .scope(v.getScope())
                        .merchantId(v.getMerchantId())
                        .discountAmount(v.getDiscountAmount().amount())
                        .currency(v.getDiscountAmount().currency())
                        .usageLimit(v.getUsageLimit())
                        .validFrom(v.getValidFrom())
                        .validUntil(v.getValidUntil())
                        .build();
        entity.setUsedCount(v.getUsedCount());
        entity.setReservedCount(v.getReservedCount());
        return entity;
    }
}

