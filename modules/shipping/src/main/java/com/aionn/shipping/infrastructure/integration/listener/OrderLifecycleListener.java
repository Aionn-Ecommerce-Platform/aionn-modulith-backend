package com.aionn.shipping.infrastructure.integration.listener;

import com.aionn.shipping.application.port.out.ShipmentPersistencePort;
import com.aionn.shipping.application.service.ShipmentCarrierOrchestrator;
import com.aionn.shipping.domain.model.Shipment;
import com.aionn.contracts.event.ordering.OrderCancelledIntegrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component("shippingOrderLifecycleListener")
@RequiredArgsConstructor
public class OrderLifecycleListener {

    private final ShipmentPersistencePort shipmentRepository;
    private final ShipmentCarrierOrchestrator shipmentCarrierOrchestrator;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderCancelled(OrderCancelledIntegrationEvent event) {
        String reason = event.cancellationType().name() + ":" + event.reasonCode();
        cancelOpenShipments(event.orderId(), reason);
    }

    private void cancelOpenShipments(String orderId, String reason) {
        for (Shipment shipment : shipmentRepository.findByOrderId(orderId)) {
            if (!shipment.isCancellable()) {
                continue;
            }
            try {
                shipmentCarrierOrchestrator.cancelShipmentSystem(shipment.getShipmentId(), reason);
            } catch (Exception ex) {
                log.warn("Cannot auto-cancel shipment {} for order {}: {}",
                        shipment.getShipmentId(), orderId, ex.getMessage());
                throw new IllegalStateException("Unable to cancel shipment " + shipment.getShipmentId(), ex);
            }
        }
    }
}
