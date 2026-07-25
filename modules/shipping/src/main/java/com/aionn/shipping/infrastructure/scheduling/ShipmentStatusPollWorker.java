package com.aionn.shipping.infrastructure.scheduling;

import com.aionn.shipping.application.port.out.CarrierClient;
import com.aionn.shipping.application.port.out.ShipmentPersistencePort;
import com.aionn.shipping.application.port.out.integration.ShippingIntegrationEventPublisherPort;
import com.aionn.shipping.domain.model.Shipment;
import com.aionn.shipping.domain.valueobject.ShipmentStatus;
import com.aionn.shipping.infrastructure.carrier.GhnStatusMapper;
import com.aionn.sharedkernel.application.port.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShipmentStatusPollWorker {

    private final ShipmentPersistencePort shipmentRepository;
    private final CarrierClient carrierClient;
    private final GhnStatusMapper statusMapper;
    private final EventPublisher eventPublisher;
    private final ShippingIntegrationEventPublisherPort integrationEventPublisher;

    public void syncOne(String shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId).orElse(null);
        if (shipment == null || shipment.getTrackingCode() == null
                || shipment.getStatus().isTerminal()) {
            return;
        }
        CarrierClient.OrderDetail detail;
        try {
            detail = carrierClient.fetchOrderDetail(shipment.getTrackingCode());
        } catch (RuntimeException ex) {
            log.warn("GHN detail failed for {} ({}): {}", shipment.getShipmentId(),
                    shipment.getTrackingCode(), ex.getMessage());
            return;
        }
        Optional<ShipmentStatus> mapped = statusMapper.map(detail.status());
        if (mapped.isEmpty()) {
            return;
        }
        try {
            apply(shipmentId, mapped.get(), detail);
        } catch (RuntimeException ex) {
            log.warn("Failed to apply polled status for {}: {}", shipmentId, ex.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void apply(String shipmentId, ShipmentStatus target, CarrierClient.OrderDetail detail) {
        Shipment shipment = shipmentRepository.findById(shipmentId).orElse(null);
        if (shipment == null || shipment.getStatus().isTerminal() || shipment.getStatus() == target) {
            return;
        }
        ShipmentStatus before = shipment.getStatus();
        switch (target) {
            case PICKED_UP -> shipment.markPickedUp(detail.warehouseId());
            case IN_TRANSIT -> shipment.updateInTransitStatus(detail.currentLocation(), detail.status());
            case OUT_FOR_DELIVERY -> shipment.markOutForDelivery(detail.shipperName(), detail.shipperPhone());
            case DELIVERED -> shipment.markDelivered(detail.signatureUrl());
            case DELIVERY_FAILED -> shipment.recordDeliveryFailure(detail.reason());
            case RETURNED -> shipment.markReturned(detail.reason());
            case CANCELLED -> shipment.cancel(detail.reason());
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

            }
        }
    }
}
