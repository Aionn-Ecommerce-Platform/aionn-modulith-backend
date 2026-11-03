package com.aionn.shipping.application.port.out.integration;

import java.time.Instant;

public interface ShippingIntegrationEventPublisherPort {

    void publishDispatched(String shipmentId, String orderId, String trackingCode);

    void publishDelivered(String shipmentId, String orderId, String signatureUrl, Instant deliveredAt);

    default void publishDelivered(
            String shipmentId,
            String orderId,
            String userId,
            String signatureUrl,
            Instant deliveredAt,
            Instant expectedDeliveryDate,
            boolean lateDelivery) {
        publishDelivered(shipmentId, orderId, signatureUrl, deliveredAt);
    }

    void publishDeliveryFailed(String shipmentId, String orderId, String reason, int attemptCount);
}
