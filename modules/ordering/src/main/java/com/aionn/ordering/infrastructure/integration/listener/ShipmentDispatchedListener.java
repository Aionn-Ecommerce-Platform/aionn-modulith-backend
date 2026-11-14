package com.aionn.ordering.infrastructure.integration.listener;

import com.aionn.contracts.event.shipping.ShipmentDispatchedIntegrationEvent;
import com.aionn.ordering.application.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShipmentDispatchedListener {

    private final OrderService orderService;

    @EventListener
    public void on(ShipmentDispatchedIntegrationEvent event) {
        try {
            orderService.markShippedFromCarrier(event.orderId(), event.shipmentId());
        } catch (RuntimeException ex) {
            log.error("Could not mark order {} shipped after shipment {} was dispatched",
                    event.orderId(), event.shipmentId(), ex);
            throw ex;
        }
    }
}
