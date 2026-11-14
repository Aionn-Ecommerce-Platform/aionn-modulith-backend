package com.aionn.ordering.infrastructure.integration.listener;

import com.aionn.contracts.event.shipping.ShipmentDeliveredIntegrationEvent;
import com.aionn.ordering.application.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShipmentDeliveredListener {

    private final OrderService orderService;

    @EventListener
    public void on(ShipmentDeliveredIntegrationEvent event) {
        try {
            orderService.completeFromCarrier(event.orderId(), event.shipmentId());
        } catch (RuntimeException ex) {
            log.error("Could not complete order {} after shipment {} delivered",
                    event.orderId(), event.shipmentId(), ex);
            throw ex;
        }
    }
}
