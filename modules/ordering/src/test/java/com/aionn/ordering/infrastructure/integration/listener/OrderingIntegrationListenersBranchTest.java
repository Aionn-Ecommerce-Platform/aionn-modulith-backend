package com.aionn.ordering.infrastructure.integration.listener;

import com.aionn.sharedkernel.integration.event.inventory.StockCommittedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.inventory.StockReservationFailedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.payment.PaymentFailedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.shipping.ShipmentDeliveredIntegrationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderingIntegrationListenersBranchTest {

    @Mock
    private com.aionn.ordering.application.service.OrderService orderService;

    @InjectMocks
    private PaymentFailedListener paymentFailedListener;

    @InjectMocks
    private ShipmentDeliveredListener shipmentDeliveredListener;

    private InventoryAuditListener auditListener = new InventoryAuditListener();

    @Test
    void inventoryAuditListenerLogsEvents() {
        StockCommittedIntegrationEvent committed = new StockCommittedIntegrationEvent(
                "ev-1", "res-1", "ord-1", "sku-1", "wh-1", 2, Instant.now());
        StockReservationFailedIntegrationEvent failed = new StockReservationFailedIntegrationEvent(
                "ev-2", "ord-1", "sku-1", "wh-1", 2, "OUT_OF_STOCK", Instant.now());

        auditListener.on(committed);
        auditListener.on(failed);
    }

    @Test
    void paymentFailedListenerHandlesErrorGracefully() {
        PaymentFailedIntegrationEvent event = new PaymentFailedIntegrationEvent(
                "ev-1", "pay-100", "ord-1", "ERR", "reason", Instant.now());
        doThrow(new RuntimeException("Error")).when(orderService).cancelOnPaymentFailure("ord-1", "ERR", "reason");

        paymentFailedListener.on(event);

        verify(orderService).cancelOnPaymentFailure("ord-1", "ERR", "reason");
    }

    @Test
    void shipmentDeliveredListenerHandlesErrorGracefully() {
        ShipmentDeliveredIntegrationEvent event = new ShipmentDeliveredIntegrationEvent(
                "ev-1", "ship-100", "ord-1", "http://sig", Instant.now(), Instant.now());
        doThrow(new RuntimeException("Error")).when(orderService).statusOf("ord-1");

        shipmentDeliveredListener.on(event);

        verify(orderService).statusOf("ord-1");
    }
}
