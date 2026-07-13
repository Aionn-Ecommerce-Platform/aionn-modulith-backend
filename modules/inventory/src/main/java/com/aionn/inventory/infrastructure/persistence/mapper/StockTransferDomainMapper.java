package com.aionn.inventory.infrastructure.persistence.mapper;

import com.aionn.inventory.domain.model.StockTransfer;
import com.aionn.inventory.domain.valueobject.StockTransferStatus;
import com.aionn.inventory.infrastructure.persistence.entity.StockTransferEntity;
import org.springframework.stereotype.Component;

@Component
public class StockTransferDomainMapper {

    public StockTransfer toDomain(StockTransferEntity e) {
        return new StockTransfer(
                e.getTransferId(),
                e.getMerchantId(),
                e.getFromWarehouseId(),
                e.getToWarehouseId(),
                e.getSkuId(),
                e.getQty(),
                StockTransferStatus.valueOf(e.getStatus()),
                e.getInitiatedAt(),
                e.getCompletedAt(),
                e.getCancelledAt());
    }

    public StockTransferEntity toEntity(StockTransfer domain, StockTransferEntity existing) {
        StockTransferEntity entity = existing != null ? existing
                : StockTransferEntity.builder()
                        .transferId(domain.getTransferId())
                        .merchantId(domain.getMerchantId())
                        .fromWarehouseId(domain.getFromWarehouseId())
                        .toWarehouseId(domain.getToWarehouseId())
                        .skuId(domain.getSkuId())
                        .qty(domain.getQty())
                        .initiatedAt(domain.getInitiatedAt())
                        .build();
        entity.setStatus(domain.getStatus().name());
        entity.setCompletedAt(domain.getCompletedAt());
        entity.setCancelledAt(domain.getCancelledAt());
        return entity;
    }
}

