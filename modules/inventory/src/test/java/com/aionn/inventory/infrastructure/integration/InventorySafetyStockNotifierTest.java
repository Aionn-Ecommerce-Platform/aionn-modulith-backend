package com.aionn.inventory.infrastructure.integration;

import com.aionn.sharedkernel.integration.event.inventory.SafetyStockBreachedIntegrationEvent;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InventorySafetyStockNotifierTest {

    @Mock
    private IntegrationEventPublisher integrationEventPublisher;

    private final Instant fixedInstant = Instant.parse("2026-01-01T12:00:00Z");
    private final Clock clock = Clock.fixed(fixedInstant, ZoneOffset.UTC);

    private InventorySafetyStockNotifier safetyStockNotifier;

    @BeforeEach
    void setUp() {
        safetyStockNotifier = new InventorySafetyStockNotifier(integrationEventPublisher, clock);
    }

    @Test
    void notifySafetyStockBreachPublishesIntegrationEvent() {
        safetyStockNotifier.notifySafetyStockBreach("M_1", "SKU_1", "WH_1", 5, 10);

        ArgumentCaptor<SafetyStockBreachedIntegrationEvent> captor =
                ArgumentCaptor.forClass(SafetyStockBreachedIntegrationEvent.class);
        verify(integrationEventPublisher).publish(captor.capture());

        SafetyStockBreachedIntegrationEvent event = captor.getValue();
        assertThat(event.merchantId()).isEqualTo("M_1");
        assertThat(event.skuId()).isEqualTo("SKU_1");
        assertThat(event.warehouseId()).isEqualTo("WH_1");
        assertThat(event.availableQty()).isEqualTo(5);
        assertThat(event.safetyStockQty()).isEqualTo(10);
        assertThat(event.occurredAt()).isEqualTo(fixedInstant);
        assertThat(event.eventId()).isNotBlank();
    }
}
