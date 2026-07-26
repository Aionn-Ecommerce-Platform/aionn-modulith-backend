package com.aionn.shipping.infrastructure.integration;

import com.aionn.sharedkernel.integration.event.shipping.ShipmentDeliveredIntegrationEvent;
import com.aionn.sharedkernel.integration.event.shipping.ShipmentDeliveryFailedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.shipping.ShipmentDispatchedIntegrationEvent;
import com.aionn.sharedkernel.integration.publisher.IntegrationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ShippingIntegrationEventPublisherTest {

    @Mock
    private IntegrationEventPublisher integrationEventPublisher;

    private Clock clock;
    private Instant fixedInstant;

    private ShippingIntegrationEventPublisher publisher;

    @BeforeEach
    void setUp() {
        fixedInstant = Instant.parse("2026-07-25T12:00:00Z");
        clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
        publisher = new ShippingIntegrationEventPublisher(integrationEventPublisher, clock);
    }

    @Test
    void publishDispatchedPublishesEvent() {
        publisher.publishDispatched("S_1", "O_1", "TR_1");

        ArgumentCaptor<ShipmentDispatchedIntegrationEvent> captor =
                ArgumentCaptor.forClass(ShipmentDispatchedIntegrationEvent.class);
        verify(integrationEventPublisher).publish(captor.capture());

        ShipmentDispatchedIntegrationEvent event = captor.getValue();
        assertThat(event.shipmentId()).isEqualTo("S_1");
        assertThat(event.orderId()).isEqualTo("O_1");
        assertThat(event.trackingCode()).isEqualTo("TR_1");
        assertThat(event.occurredAt()).isEqualTo(fixedInstant);
    }

    @Test
    void publishDeliveredPublishesEvent() {
        Instant deliveredAt = Instant.parse("2026-07-25T11:00:00Z");
        publisher.publishDelivered("S_1", "O_1", "sig-url", deliveredAt);

        ArgumentCaptor<ShipmentDeliveredIntegrationEvent> captor =
                ArgumentCaptor.forClass(ShipmentDeliveredIntegrationEvent.class);
        verify(integrationEventPublisher).publish(captor.capture());

        ShipmentDeliveredIntegrationEvent event = captor.getValue();
        assertThat(event.shipmentId()).isEqualTo("S_1");
        assertThat(event.orderId()).isEqualTo("O_1");
        assertThat(event.signatureUrl()).isEqualTo("sig-url");
        assertThat(event.deliveredAt()).isEqualTo(deliveredAt);
        assertThat(event.occurredAt()).isEqualTo(fixedInstant);
    }

    @Test
    void publishDeliveryFailedPublishesEvent() {
        publisher.publishDeliveryFailed("S_1", "O_1", "Failed", 2);

        ArgumentCaptor<ShipmentDeliveryFailedIntegrationEvent> captor =
                ArgumentCaptor.forClass(ShipmentDeliveryFailedIntegrationEvent.class);
        verify(integrationEventPublisher).publish(captor.capture());

        ShipmentDeliveryFailedIntegrationEvent event = captor.getValue();
        assertThat(event.shipmentId()).isEqualTo("S_1");
        assertThat(event.orderId()).isEqualTo("O_1");
        assertThat(event.reason()).isEqualTo("Failed");
        assertThat(event.attemptCount()).isEqualTo(2);
        assertThat(event.occurredAt()).isEqualTo(fixedInstant);
    }
}
