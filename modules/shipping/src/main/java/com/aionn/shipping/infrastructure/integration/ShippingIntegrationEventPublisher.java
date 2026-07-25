package com.aionn.shipping.infrastructure.integration;

import com.aionn.shipping.application.port.out.integration.ShippingIntegrationEventPublisherPort;
import com.aionn.sharedkernel.integration.event.shipping.ShipmentDeliveredIntegrationEvent;
import com.aionn.sharedkernel.integration.event.shipping.ShipmentDeliveryFailedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.shipping.ShipmentDispatchedIntegrationEvent;
import com.aionn.sharedkernel.integration.publisher.IntegrationEventPublisher;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ShippingIntegrationEventPublisher implements ShippingIntegrationEventPublisherPort {

    private final IntegrationEventPublisher integrationEventPublisher;
    private final Clock clock;

    @Override
    public void publishDispatched(String shipmentId, String orderId, String trackingCode) {
        integrationEventPublisher.publish(new ShipmentDispatchedIntegrationEvent(
                IdGenerator.ulid(), shipmentId, orderId, trackingCode, Instant.now(clock)));
    }

    @Override
    public void publishDelivered(String shipmentId, String orderId, String signatureUrl, Instant deliveredAt) {
        integrationEventPublisher.publish(new ShipmentDeliveredIntegrationEvent(
                IdGenerator.ulid(), shipmentId, orderId, signatureUrl, deliveredAt, Instant.now(clock)));
    }

    @Override
    public void publishDeliveryFailed(String shipmentId, String orderId, String reason, int attemptCount) {
        integrationEventPublisher.publish(new ShipmentDeliveryFailedIntegrationEvent(
                IdGenerator.ulid(), shipmentId, orderId, reason, attemptCount, Instant.now(clock)));
    }
}
