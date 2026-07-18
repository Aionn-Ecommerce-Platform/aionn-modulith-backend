package com.aionn.inventory.infrastructure.integration.inventory;

import com.aionn.inventory.domain.event.InventoryItemEvents;
import com.aionn.inventory.domain.event.StockReservationEvents;
import com.aionn.sharedkernel.integration.event.inventory.SafetyStockBreachedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.inventory.StockCommittedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.inventory.StockReleasedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.inventory.StockReservationFailedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.inventory.StockReservedIntegrationEvent;
import com.aionn.sharedkernel.util.IdGenerator;
import org.springframework.stereotype.Component;

@Component
public class InventoryIntegrationEventMapper {

    public StockReservedIntegrationEvent toIntegrationEvent(StockReservationEvents.StockReserved domainEvent) {
        return new StockReservedIntegrationEvent(
                IdGenerator.ulid(),
                domainEvent.reservationId(),
                domainEvent.skuId(),
                domainEvent.warehouseId(),
                null,
                domainEvent.qty(),
                domainEvent.expiresAt(),
                domainEvent.occurredAt());
    }

    public StockReservationFailedIntegrationEvent toIntegrationEvent(
            StockReservationEvents.StockReservationFailed domainEvent) {
        return new StockReservationFailedIntegrationEvent(
                IdGenerator.ulid(),
                domainEvent.skuId(),
                domainEvent.warehouseId(),
                null,
                domainEvent.qty(),
                domainEvent.reason(),
                domainEvent.occurredAt());
    }

    public StockCommittedIntegrationEvent toIntegrationEvent(StockReservationEvents.StockCommitted domainEvent) {
        return new StockCommittedIntegrationEvent(
                IdGenerator.ulid(),
                domainEvent.reservationId(),
                domainEvent.skuId(),
                domainEvent.warehouseId(),
                domainEvent.orderId(),
                domainEvent.qty(),
                domainEvent.occurredAt());
    }

    public StockReleasedIntegrationEvent toIntegrationEvent(StockReservationEvents.StockReleased domainEvent) {
        return new StockReleasedIntegrationEvent(
                IdGenerator.ulid(),
                domainEvent.reservationId(),
                domainEvent.skuId(),
                domainEvent.warehouseId(),
                domainEvent.orderId(),
                domainEvent.qty(),
                domainEvent.reason(),
                domainEvent.occurredAt());
    }

    public SafetyStockBreachedIntegrationEvent toIntegrationEvent(
            String merchantId, InventoryItemEvents.SafetyStockBreached domainEvent) {
        return new SafetyStockBreachedIntegrationEvent(
                IdGenerator.ulid(),
                domainEvent.occurredAt(),
                merchantId,
                domainEvent.skuId(),
                domainEvent.warehouseId(),
                domainEvent.availableQty(),
                domainEvent.safetyStockQty());
    }
}
