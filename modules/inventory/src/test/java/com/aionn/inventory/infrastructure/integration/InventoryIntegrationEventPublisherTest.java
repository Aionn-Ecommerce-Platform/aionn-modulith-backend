package com.aionn.inventory.infrastructure.integration;

import com.aionn.inventory.application.port.out.WarehousePersistencePort;
import com.aionn.inventory.domain.event.InventoryItemEvents;
import com.aionn.inventory.domain.event.StockReservationEvents;
import com.aionn.inventory.domain.model.Warehouse;
import com.aionn.sharedkernel.integration.event.inventory.*;
import com.aionn.sharedkernel.integration.publisher.IntegrationEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryIntegrationEventPublisherTest {

    @Mock
    private IntegrationEventPublisher integrationEventPublisher;

    @Mock
    private InventoryIntegrationEventMapper mapper;

    @Mock
    private WarehousePersistencePort warehouseRepository;

    @InjectMocks
    private InventoryIntegrationEventPublisher publisher;

    private static final Instant OCCURRED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void onStockReservedPublishesIntegrationEvent() {
        StockReservationEvents.StockReserved domainEvent = new StockReservationEvents.StockReserved(
                "res-1", "sku-1", "wh-1", 10, OCCURRED_AT.plusSeconds(3600), OCCURRED_AT
        );
        StockReservedIntegrationEvent integrationEvent = mock(StockReservedIntegrationEvent.class);
        when(mapper.toIntegrationEvent(domainEvent)).thenReturn(integrationEvent);

        publisher.onStockReserved(domainEvent);

        verify(integrationEventPublisher).publish(integrationEvent);
    }

    @Test
    void onStockReservationFailedPublishesIntegrationEvent() {
        StockReservationEvents.StockReservationFailed domainEvent = new StockReservationEvents.StockReservationFailed(
                "res-1", "sku-1", "wh-1", 10, "reason", OCCURRED_AT
        );
        StockReservationFailedIntegrationEvent integrationEvent = mock(StockReservationFailedIntegrationEvent.class);
        when(mapper.toIntegrationEvent(domainEvent)).thenReturn(integrationEvent);

        publisher.onStockReservationFailed(domainEvent);

        verify(integrationEventPublisher).publish(integrationEvent);
    }

    @Test
    void onStockCommittedPublishesIntegrationEvent() {
        StockReservationEvents.StockCommitted domainEvent = new StockReservationEvents.StockCommitted(
                "res-1", "sku-1", "wh-1", "order-1", 10, OCCURRED_AT
        );
        StockCommittedIntegrationEvent integrationEvent = mock(StockCommittedIntegrationEvent.class);
        when(mapper.toIntegrationEvent(domainEvent)).thenReturn(integrationEvent);

        publisher.onStockCommitted(domainEvent);

        verify(integrationEventPublisher).publish(integrationEvent);
    }

    @Test
    void onStockReleasedPublishesIntegrationEvent() {
        StockReservationEvents.StockReleased domainEvent = new StockReservationEvents.StockReleased(
                "res-1", "sku-1", "wh-1", "order-1", 10, "reason", OCCURRED_AT
        );
        StockReleasedIntegrationEvent integrationEvent = mock(StockReleasedIntegrationEvent.class);
        when(mapper.toIntegrationEvent(domainEvent)).thenReturn(integrationEvent);

        publisher.onStockReleased(domainEvent);

        verify(integrationEventPublisher).publish(integrationEvent);
    }

    @Test
    void onSafetyStockBreachedPublishesIntegrationEventWhenWarehouseExists() {
        InventoryItemEvents.SafetyStockBreached domainEvent = new InventoryItemEvents.SafetyStockBreached(
                "sku-1", "wh-1", 5, 10, OCCURRED_AT
        );
        Warehouse warehouse = mock(Warehouse.class);
        when(warehouse.getMerchantId()).thenReturn("merchant-1");
        when(warehouseRepository.findById("wh-1")).thenReturn(Optional.of(warehouse));

        SafetyStockBreachedIntegrationEvent integrationEvent = mock(SafetyStockBreachedIntegrationEvent.class);
        when(mapper.toIntegrationEvent("merchant-1", domainEvent)).thenReturn(integrationEvent);

        publisher.onSafetyStockBreached(domainEvent);

        verify(integrationEventPublisher).publish(integrationEvent);
    }

    @Test
    void onSafetyStockBreachedDoesNotPublishWhenWarehouseNotFound() {
        InventoryItemEvents.SafetyStockBreached domainEvent = new InventoryItemEvents.SafetyStockBreached(
                "sku-1", "wh-1", 5, 10, OCCURRED_AT
        );
        when(warehouseRepository.findById("wh-1")).thenReturn(Optional.empty());

        publisher.onSafetyStockBreached(domainEvent);

        verifyNoInteractions(integrationEventPublisher);
        verify(mapper, never()).toIntegrationEvent(any(), any());
    }
}
