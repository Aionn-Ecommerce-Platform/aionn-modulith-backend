package com.aionn.shipping.infrastructure.integration;

import com.aionn.shipping.application.port.out.integration.ShippingIntegrationEventPublisherPort;
import com.aionn.contracts.event.shipping.ShipmentDeliveredIntegrationEvent;
import com.aionn.contracts.event.shipping.ShipmentDeliveryFailedIntegrationEvent;
import com.aionn.contracts.event.shipping.ShipmentDispatchedIntegrationEvent;
import com.aionn.contracts.publisher.IntegrationEventPublisher;
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
                IdGenerator.ulid(), shipmentId, orderId, trackingCode, clock.instant()));
    }

    @Override
    public void publishDelivered(String shipmentId, String orderId, String signatureUrl, Instant deliveredAt) {
        publishDelivered(shipmentId, orderId, null, signatureUrl, deliveredAt, null, false);
    }

    @Override
    public void publishDelivered(
            String shipmentId,
            String orderId,
            String userId,
            String signatureUrl,
            Instant deliveredAt,
            Instant expectedDeliveryDate,
            boolean lateDelivery) {
        integrationEventPublisher.publish(new ShipmentDeliveredIntegrationEvent(
                IdGenerator.ulid(),
                shipmentId,
                orderId,
                userId,
                signatureUrl,
                deliveredAt,
                expectedDeliveryDate,
                lateDelivery,
                clock.instant()));
    }

    @Override
    public void publishDeliveryFailed(String shipmentId, String orderId, String reason, int attemptCount) {
        integrationEventPublisher.publish(new ShipmentDeliveryFailedIntegrationEvent(
                IdGenerator.ulid(), shipmentId, orderId, reason, attemptCount, clock.instant()));
    }
}
