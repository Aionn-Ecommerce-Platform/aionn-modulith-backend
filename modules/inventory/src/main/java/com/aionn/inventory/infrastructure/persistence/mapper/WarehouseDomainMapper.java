package com.aionn.inventory.infrastructure.persistence.mapper;

import com.aionn.inventory.domain.model.Warehouse;
import com.aionn.inventory.domain.valueobject.WarehouseStatus;
import com.aionn.inventory.infrastructure.persistence.entity.WarehouseEntity;
import org.springframework.stereotype.Component;

@Component
public class WarehouseDomainMapper {

    public Warehouse toDomain(WarehouseEntity e) {
        return new Warehouse(
                e.getWarehouseId(),
                e.getMerchantId(),
                e.getAddress(),
                e.getPriorityLevel(),
                WarehouseStatus.valueOf(e.getStatus()),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }

    public WarehouseEntity toEntity(Warehouse domain, WarehouseEntity existing) {
        WarehouseEntity entity = existing != null ? existing
                : WarehouseEntity.builder()
                        .warehouseId(domain.getWarehouseId())
                        .build();
        entity.setMerchantId(domain.getMerchantId());
        entity.setAddress(domain.getAddress());
        entity.setPriorityLevel(domain.getPriorityLevel());
        entity.setStatus(domain.getStatus().name());
        return entity;
    }
}

