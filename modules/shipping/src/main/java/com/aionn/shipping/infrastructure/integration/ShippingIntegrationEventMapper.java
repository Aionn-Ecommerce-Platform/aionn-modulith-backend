package com.aionn.shipping.infrastructure.integration;

import com.aionn.shipping.domain.event.ShipmentEvents;
import com.aionn.sharedkernel.integration.event.shipping.ShipmentCancelledIntegrationEvent;
import com.aionn.sharedkernel.integration.event.shipping.ShipmentDeliveredIntegrationEvent;
import com.aionn.sharedkernel.integration.event.shipping.ShipmentDispatchedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.shipping.ShipmentRegisteredIntegrationEvent;
import org.springframework.stereotype.Component;

@Component
public class ShippingIntegrationEventMapper {

    public ShipmentRegisteredIntegrationEvent toIntegrationEvent(ShipmentEvents.ShipmentRegistered domainEvent) {
        return new ShipmentRegisteredIntegrationEvent(
                null,
                domainEvent.shipmentId(),
                domainEvent.orderId(),
                domainEvent.trackingCode(),
                domainEvent.carrierOrderId(),
                domainEvent.occurredAt());
    }

    public ShipmentDispatchedIntegrationEvent toIntegrationEvent(ShipmentEvents.ShipmentOutForDelivery domainEvent) {
        return new ShipmentDispatchedIntegrationEvent(
                null,
                domainEvent.shipmentId(),
                domainEvent.orderId(),
                domainEvent.shipperName(),
                domainEvent.occurredAt());
    }

    public ShipmentDeliveredIntegrationEvent toIntegrationEvent(ShipmentEvents.ShipmentDelivered domainEvent) {
        return new ShipmentDeliveredIntegrationEvent(
                null,
                domainEvent.shipmentId(),
                domainEvent.orderId(),
                domainEvent.signatureUrl(),
                domainEvent.deliveredAt(),
                domainEvent.occurredAt());
    }

    public ShipmentCancelledIntegrationEvent toIntegrationEvent(ShipmentEvents.ShipmentCancelled domainEvent) {
        return new ShipmentCancelledIntegrationEvent(
                null,
                domainEvent.shipmentId(),
                domainEvent.orderId(),
                domainEvent.reason(),
                domainEvent.occurredAt());
    }
}
