package com.aionn.inventory.infrastructure.integration;

import com.aionn.inventory.domain.event.InventoryItemEvents;
import com.aionn.inventory.domain.event.StockReservationEvents;
import com.aionn.sharedkernel.integration.event.inventory.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InventoryIntegrationEventMapperTest {

    private final InventoryIntegrationEventMapper mapper = new InventoryIntegrationEventMapper();
    private static final Instant OCCURRED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void mapsStockReservedCorrectly() {
        StockReservationEvents.StockReserved domainEvent = new StockReservationEvents.StockReserved(
                "res-1", "sku-1", "wh-1", 10, OCCURRED_AT.plusSeconds(3600), OCCURRED_AT
        );

        StockReservedIntegrationEvent event = mapper.toIntegrationEvent(domainEvent);
        assertNotNull(event.eventId());
        assertEquals("res-1", event.reservationId());
        assertEquals("sku-1", event.skuId());
        assertEquals("wh-1", event.warehouseId());
        assertEquals(10, event.quantity());
        assertEquals(OCCURRED_AT.plusSeconds(3600), event.expiresAt());
        assertEquals(OCCURRED_AT, event.occurredAt());
    }

    @Test
    void mapsStockReservationFailedCorrectly() {
        StockReservationEvents.StockReservationFailed domainEvent = new StockReservationEvents.StockReservationFailed(
                "res-1", "sku-1", "wh-1", 10, "out of stock", OCCURRED_AT
        );

        StockReservationFailedIntegrationEvent event = mapper.toIntegrationEvent(domainEvent);
        assertNotNull(event.eventId());
        assertEquals("sku-1", event.skuId());
        assertEquals("wh-1", event.warehouseId());
        assertEquals(10, event.quantity());
        assertEquals("out of stock", event.reason());
        assertEquals(OCCURRED_AT, event.occurredAt());
    }

    @Test
    void mapsStockCommittedCorrectly() {
        StockReservationEvents.StockCommitted domainEvent = new StockReservationEvents.StockCommitted(
                "res-1", "sku-1", "wh-1", "order-1", 10, OCCURRED_AT
        );

        StockCommittedIntegrationEvent event = mapper.toIntegrationEvent(domainEvent);
        assertNotNull(event.eventId());
        assertEquals("res-1", event.reservationId());
        assertEquals("sku-1", event.skuId());
        assertEquals("wh-1", event.warehouseId());
        assertEquals("order-1", event.orderId());
        assertEquals(10, event.quantity());
        assertEquals(OCCURRED_AT, event.occurredAt());
    }

    @Test
    void mapsStockReleasedCorrectly() {
        StockReservationEvents.StockReleased domainEvent = new StockReservationEvents.StockReleased(
                "res-1", "sku-1", "wh-1", "order-1", 10, "expired", OCCURRED_AT
        );

        StockReleasedIntegrationEvent event = mapper.toIntegrationEvent(domainEvent);
        assertNotNull(event.eventId());
        assertEquals("res-1", event.reservationId());
        assertEquals("sku-1", event.skuId());
        assertEquals("wh-1", event.warehouseId());
        assertEquals("order-1", event.orderId());
        assertEquals(10, event.quantity());
        assertEquals("expired", event.reason());
        assertEquals(OCCURRED_AT, event.occurredAt());
    }

    @Test
    void mapsSafetyStockBreachedCorrectly() {
        InventoryItemEvents.SafetyStockBreached domainEvent = new InventoryItemEvents.SafetyStockBreached(
                "sku-1", "wh-1", 5, 10, OCCURRED_AT
        );

        SafetyStockBreachedIntegrationEvent event = mapper.toIntegrationEvent("merchant-1", domainEvent);
        assertNotNull(event.eventId());
        assertEquals("merchant-1", event.merchantId());
        assertEquals("sku-1", event.skuId());
        assertEquals("wh-1", event.warehouseId());
        assertEquals(5, event.availableQty());
        assertEquals(10, event.safetyStockQty());
        assertEquals(OCCURRED_AT, event.occurredAt());
    }
}
