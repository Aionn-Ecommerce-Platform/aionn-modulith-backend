package com.aionn.ordering.infrastructure.integration.listener;

import com.aionn.ordering.application.dto.order.command.ConfirmDeliveredCommand;
import com.aionn.ordering.application.service.OrderService;
import com.aionn.ordering.domain.valueobject.OrderStatus;
import com.aionn.sharedkernel.integration.event.shipping.ShipmentDeliveredIntegrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShipmentDeliveredListener {

    private final OrderService orderService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(ShipmentDeliveredIntegrationEvent event) {
        try {
            OrderStatus status = orderService.statusOf(event.orderId());
            if (status == OrderStatus.SHIPPED) {
                orderService.complete(new ConfirmDeliveredCommand(event.orderId()));
            }
        } catch (RuntimeException ex) {
            log.error("Could not complete order {} after shipment {} delivered",
                    event.orderId(), event.shipmentId(), ex);
        }
    }
}
