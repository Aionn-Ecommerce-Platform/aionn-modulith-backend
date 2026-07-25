package com.aionn.shipping.infrastructure.persistence.mapper;

import com.aionn.shipping.domain.model.ShippingRate;
import com.aionn.shipping.infrastructure.persistence.entity.ShippingRateEntity;
import org.springframework.stereotype.Component;

@Component
public class ShippingRateDomainMapper {

    public ShippingRate toDomain(ShippingRateEntity e) {
        return new ShippingRate(
                e.getRateId(), e.getZoneCode(), e.getBaseFee(), e.getCurrency(), e.getCondition(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    public ShippingRateEntity toEntity(ShippingRate r, ShippingRateEntity existing) {
        ShippingRateEntity entity = existing != null ? existing
                : ShippingRateEntity.builder()
                        .rateId(r.getRateId())
                        .zoneCode(r.getZoneCode())
                        .currency(r.getCurrency())
                        .build();
        entity.setBaseFee(r.getBaseFee());
        entity.setCondition(r.getCondition());
        return entity;
    }
}

