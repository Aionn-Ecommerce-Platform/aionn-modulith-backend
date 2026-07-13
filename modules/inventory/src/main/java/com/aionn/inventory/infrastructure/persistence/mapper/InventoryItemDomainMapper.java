package com.aionn.inventory.infrastructure.persistence.mapper;

import com.aionn.inventory.domain.model.InventoryItem;
import com.aionn.inventory.domain.valueobject.InventoryItemKey;
import com.aionn.inventory.infrastructure.persistence.entity.InventoryItemEntity;
import org.springframework.stereotype.Component;

@Component
public class InventoryItemDomainMapper {

    public InventoryItem toDomain(InventoryItemEntity e) {
        return new InventoryItem(
                new InventoryItemKey(e.getId().getSkuId(), e.getId().getWarehouseId()),
                e.getPhysicalQty(),
                e.getAvailableQty(),
                e.getSafetyStockQty(),
                e.isLocked(),
                e.getBatchNo(),
                e.getExpiryDate(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }

    public InventoryItemEntity toEntity(InventoryItem domain, InventoryItemEntity existing) {
        InventoryItemEntity entity = existing != null ? existing
                : InventoryItemEntity.builder()
                        .id(new InventoryItemEntity.InventoryItemId(
                                domain.getKey().skuId(), domain.getKey().warehouseId()))
                        .build();
        entity.setPhysicalQty(domain.getPhysicalQty());
        entity.setAvailableQty(domain.getAvailableQty());
        entity.setSafetyStockQty(domain.getSafetyStockQty());
        entity.setLocked(domain.isLocked());
        entity.setBatchNo(domain.getBatchNo());
        entity.setExpiryDate(domain.getExpiryDate());
        return entity;
    }
}

