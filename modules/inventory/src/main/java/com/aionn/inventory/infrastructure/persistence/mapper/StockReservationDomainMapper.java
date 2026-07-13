package com.aionn.inventory.infrastructure.persistence.mapper;

import com.aionn.inventory.domain.model.StockReservation;
import com.aionn.inventory.domain.valueobject.ReservationStatus;
import com.aionn.inventory.infrastructure.persistence.entity.StockReservationEntity;
import org.springframework.stereotype.Component;

@Component
public class StockReservationDomainMapper {

    public StockReservation toDomain(StockReservationEntity e) {
        return new StockReservation(
                e.getReservationId(),
                e.getSkuId(),
                e.getWarehouseId(),
                e.getOrderId(),
                e.getQty(),
                ReservationStatus.valueOf(e.getStatus()),
                e.getReservedAt(),
                e.getExpiresAt(),
                e.getDecidedAt());
    }

    public StockReservationEntity toEntity(StockReservation domain, StockReservationEntity existing) {
        StockReservationEntity entity = existing != null ? existing
                : StockReservationEntity.builder()
                        .reservationId(domain.getReservationId())
                        .skuId(domain.getSkuId())
                        .warehouseId(domain.getWarehouseId())
                        .orderId(domain.getOrderId())
                        .qty(domain.getQty())
                        .reservedAt(domain.getReservedAt())
                        .expiresAt(domain.getExpiresAt())
                        .build();
        entity.setStatus(domain.getStatus().name());
        entity.setDecidedAt(domain.getDecidedAt());
        return entity;
    }
}

