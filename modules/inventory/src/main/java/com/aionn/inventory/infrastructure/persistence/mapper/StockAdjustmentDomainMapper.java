package com.aionn.inventory.infrastructure.persistence.mapper;

import com.aionn.inventory.domain.model.StockAdjustment;
import com.aionn.inventory.domain.valueobject.AdjustmentType;
import com.aionn.inventory.infrastructure.persistence.entity.StockAdjustmentEntity;
import org.springframework.stereotype.Component;

@Component
public class StockAdjustmentDomainMapper {

    public StockAdjustment toDomain(StockAdjustmentEntity e) {
        return new StockAdjustment(
                e.getAdjId(),
                e.getSkuId(),
                e.getWarehouseId(),
                e.getQty(),
                AdjustmentType.valueOf(e.getType()),
                e.getReason(),
                e.getOrderId(),
                e.getOccurredAt());
    }

    public StockAdjustmentEntity toEntity(StockAdjustment domain) {
        return StockAdjustmentEntity.builder()
                .adjId(domain.getAdjId())
                .skuId(domain.getSkuId())
                .warehouseId(domain.getWarehouseId())
                .qty(domain.getQty())
                .type(domain.getType().name())
                .reason(domain.getReason())
                .orderId(domain.getOrderId())
                .occurredAt(domain.getOccurredAt())
                .build();
    }
}

