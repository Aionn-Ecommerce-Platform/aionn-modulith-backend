package com.aionn.inventory.infrastructure.notifier;

import com.aionn.inventory.infrastructure.integration.InventoryOutboundOrderNotifier;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ResilientOutboundOrderNotifierTest {

    @Mock
    IntegrationEventPublisher integrationEventPublisher;
    @Mock
    Clock clock;

    InventoryOutboundOrderNotifier notifier;

    private final Instant fixedInstant = Instant.parse("2026-07-12T15:00:00Z");

    @BeforeEach
    void setUp() {
        notifier = new InventoryOutboundOrderNotifier(
            integrationEventPublisher,
            Clock.fixed(fixedInstant, java.time.ZoneOffset.UTC)
        );
    }

    @Test
    void notifyOutboundPublishesStockCommittedEvent() {
        notifier.notifyOutbound("ORDER_1", "RES_1", "SKU_1", "WH_1", 3);

        ArgumentCaptor<StockCommittedIntegrationEvent> captor =
                ArgumentCaptor.forClass(StockCommittedIntegrationEvent.class);
        verify(integrationEventPublisher).publish(captor.capture());

        StockCommittedIntegrationEvent ev = captor.getValue();
        assertThat(ev.reservationId()).isEqualTo("RES_1");
        assertThat(ev.skuId()).isEqualTo("SKU_1");
        assertThat(ev.warehouseId()).isEqualTo("WH_1");
        assertThat(ev.orderId()).isEqualTo("ORDER_1");
        assertThat(ev.quantity()).isEqualTo(3);
        assertThat(ev.occurredAt()).isNotNull();
        assertThat(ev.eventId()).isNotNull();
    }

    @Test
    void notifyReservationFailedPublishesFailureEvent() {
        notifier.notifyReservationFailed("ORDER_2", "SKU_2", "WH_2", 5, "Insufficient stock");

        ArgumentCaptor<StockReservationFailedIntegrationEvent> captor =
                ArgumentCaptor.forClass(StockReservationFailedIntegrationEvent.class);
        verify(integrationEventPublisher).publish(captor.capture());

        StockReservationFailedIntegrationEvent ev = captor.getValue();
        assertThat(ev.skuId()).isEqualTo("SKU_2");
        assertThat(ev.warehouseId()).isEqualTo("WH_2");
        assertThat(ev.orderId()).isEqualTo("ORDER_2");
        assertThat(ev.quantity()).isEqualTo(5);
        assertThat(ev.reason()).isEqualTo("Insufficient stock");
    }

    @Test
    void noPublicationWhenNotifierIsNotInvoked() {
        verifyNoInteractions(integrationEventPublisher);
    }
}
