package com.aionn.shipping.infrastructure.scheduling;

import com.aionn.shipping.application.port.out.CarrierClient;
import com.aionn.shipping.application.port.out.ShipmentPersistencePort;
import com.aionn.shipping.application.port.out.integration.ShippingIntegrationEventPublisherPort;
import com.aionn.shipping.domain.model.Shipment;
import com.aionn.shipping.domain.valueobject.ShipmentStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Component
@RequiredArgsConstructor
public class ShipmentStatusApplier {

    private final ShipmentPersistencePort shipmentRepository;
    private final EventPublisher eventPublisher;
    private final ShippingIntegrationEventPublisherPort integrationEventPublisher;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void apply(String shipmentId, ShipmentStatus target, CarrierClient.OrderDetail detail) {
        Shipment shipment = shipmentRepository.findById(shipmentId).orElse(null);
        if (shipment == null || shipment.getStatus().isTerminal() || shipment.getStatus() == target) {
            return;
        }
        ShipmentStatus before = shipment.getStatus();
        switch (target) {
            case PICKED_UP -> shipment.markPickedUp(detail.warehouseId(), clock);
            case IN_TRANSIT -> shipment.updateInTransitStatus(detail.currentLocation(), detail.status(), clock);
            case OUT_FOR_DELIVERY ->
                shipment.markOutForDelivery(detail.shipperName(), detail.shipperPhone(), clock);
            case DELIVERED -> shipment.markDelivered(detail.signatureUrl(), clock);
            case DELIVERY_FAILED -> shipment.recordDeliveryFailure(detail.reason(), clock);
            case RETURNED -> shipment.markReturned(detail.reason(), clock);
            case CANCELLED -> shipment.cancel(detail.reason(), clock);
            default -> {
                return;
            }
        }
        Shipment saved = shipmentRepository.save(shipment);
        eventPublisher.publish(shipment.pullEvents());
        publishIntegration(saved, before, target, detail);
    }

    private void publishIntegration(Shipment saved, ShipmentStatus before, ShipmentStatus after,
            CarrierClient.OrderDetail detail) {
        if (before == after) {
            return;
        }
        switch (after) {
            case PICKED_UP -> integrationEventPublisher.publishDispatched(
                    saved.getShipmentId(), saved.getOrderId(), saved.getTrackingCode());
            case DELIVERED -> integrationEventPublisher.publishDelivered(
                    saved.getShipmentId(), saved.getOrderId(), detail.signatureUrl(), saved.getDeliveredAt());
            case DELIVERY_FAILED -> integrationEventPublisher.publishDeliveryFailed(
                    saved.getShipmentId(), saved.getOrderId(), detail.reason(), saved.getAttemptCount());
            default -> {
                /* in-transit / out-for-delivery / returned do not surface to other contexts */
            }
        }
    }
}
