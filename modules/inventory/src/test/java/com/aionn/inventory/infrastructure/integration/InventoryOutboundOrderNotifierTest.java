package com.aionn.inventory.infrastructure.integration;

import com.aionn.sharedkernel.integration.event.inventory.StockCommittedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.inventory.StockReservationFailedIntegrationEvent;
import com.aionn.sharedkernel.integration.publisher.IntegrationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InventoryOutboundOrderNotifierTest {

    @Mock
    private IntegrationEventPublisher integrationEventPublisher;

    private Clock clock;
    private InventoryOutboundOrderNotifier notifier;

    private static final Instant FIXED_NOW = Instant.parse("2026-01-01T00:00:00Z");

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        notifier = new InventoryOutboundOrderNotifier(integrationEventPublisher, clock);
    }

    @Test
    void notifyOutboundPublishesCorrectEvent() {
        notifier.notifyOutbound("order-1", "res-1", "sku-1", "wh-1", 5);

        ArgumentCaptor<StockCommittedIntegrationEvent> captor = ArgumentCaptor.forClass(StockCommittedIntegrationEvent.class);
        verify(integrationEventPublisher).publish(captor.capture());

        StockCommittedIntegrationEvent event = captor.getValue();
        assertNotNull(event.eventId());
        assertEquals("res-1", event.reservationId());
        assertEquals("sku-1", event.skuId());
        assertEquals("wh-1", event.warehouseId());
        assertEquals("order-1", event.orderId());
        assertEquals(5, event.quantity());
        assertEquals(FIXED_NOW, event.occurredAt());
    }

    @Test
    void notifyReservationFailedPublishesCorrectEvent() {
        notifier.notifyReservationFailed("order-1", "sku-1", "wh-1", 10, "out of stock");

        ArgumentCaptor<StockReservationFailedIntegrationEvent> captor = ArgumentCaptor.forClass(StockReservationFailedIntegrationEvent.class);
        verify(integrationEventPublisher).publish(captor.capture());

        StockReservationFailedIntegrationEvent event = captor.getValue();
        assertNotNull(event.eventId());
        assertEquals("sku-1", event.skuId());
        assertEquals("wh-1", event.warehouseId());
        assertEquals("order-1", event.orderId());
        assertEquals(10, event.quantity());
        assertEquals("out of stock", event.reason());
        assertEquals(FIXED_NOW, event.occurredAt());
    }
}
