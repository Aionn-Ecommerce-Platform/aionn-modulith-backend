package com.aionn.ordering.infrastructure.integration.listener;

import com.aionn.contracts.event.shipping.ShipmentCancelledIntegrationEvent;
import com.aionn.contracts.event.shipping.ShipmentDeliveryFailedIntegrationEvent;
import com.aionn.contracts.event.shipping.ShipmentReturnedIntegrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShipmentFailureListener {

    @EventListener
    public void on(ShipmentDeliveryFailedIntegrationEvent event) {
        log.warn("Shipment {} delivery attempt {} failed for order {}: {}",
                event.shipmentId(), event.attemptCount(), event.orderId(), event.reason());
    }

    @EventListener
    public void on(ShipmentReturnedIntegrationEvent event) {
        log.error("Shipment {} was returned for order {}: {}; manual return/refund review required",
                event.shipmentId(), event.orderId(), event.reason());
    }

    @EventListener
    public void on(ShipmentCancelledIntegrationEvent event) {
        log.warn("Shipment {} cancelled for order {}: {}",
                event.shipmentId(), event.orderId(), event.reason());
    }
}
