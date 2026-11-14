package com.aionn.notification.infrastructure.listener;

import com.aionn.contracts.event.shipping.ShipmentDeliveryFailedIntegrationEvent;
import com.aionn.contracts.event.shipping.ShipmentReturnedIntegrationEvent;
import com.aionn.contracts.port.catalog.MerchantQueryPort;
import com.aionn.contracts.port.ordering.OrderQueryPort;
import com.aionn.notification.application.service.NotificationDeliveryOrchestrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShippingEventListenerTest {

    @Mock
    private NotificationDeliveryOrchestrator deliveryOrchestrator;

    @Mock
    private OrderQueryPort orderQueryPort;

    @Mock
    private MerchantQueryPort merchantQueryPort;

    @Test
    void queuesDeliveryFailureNotificationForShipmentOwner() {
        new ShippingEventListener(deliveryOrchestrator, orderQueryPort, merchantQueryPort)
                .on(new ShipmentDeliveryFailedIntegrationEvent(
                        "evt-1", "ship-1", "order-1", "user-1", "customer unreachable", 2, Instant.now()));

        verify(deliveryOrchestrator).enqueueByEvent(any());
        verify(deliveryOrchestrator).deliverQueuedAsync(any());
    }

    @Test
    void fallsBackToOrderOwnerWhenEventHasNoUser() {
        when(orderQueryPort.findOrderSummary("order-1")).thenReturn(Optional.of(
                new OrderQueryPort.OrderSummary("order-1", "user-1", "merchant-1", null, "VND", "SHIPPED")));

        new ShippingEventListener(deliveryOrchestrator, orderQueryPort, merchantQueryPort)
                .on(new ShipmentDeliveryFailedIntegrationEvent(
                        "evt-1", "ship-1", "order-1", null, "customer unreachable", 2, Instant.now()));

        verify(deliveryOrchestrator).enqueueByEvent(any());
        verify(deliveryOrchestrator).deliverQueuedAsync(any());
    }

    @Test
    void skipsWhenRecipientCannotBeResolved() {
        when(orderQueryPort.findOrderSummary("order-1")).thenReturn(Optional.empty());

        new ShippingEventListener(deliveryOrchestrator, orderQueryPort, merchantQueryPort)
                .on(new ShipmentDeliveryFailedIntegrationEvent(
                        "evt-1", "ship-1", "order-1", null, "customer unreachable", 2, Instant.now()));

        verify(deliveryOrchestrator, never()).enqueueByEvent(any());
    }

    @Test
    void queuesReturnedNotificationForMerchantOwner() {
        when(merchantQueryPort.findOwnerIdByMerchantId("merchant-1"))
                .thenReturn(Optional.of("owner-1"));

        new ShippingEventListener(deliveryOrchestrator, orderQueryPort, merchantQueryPort)
                .on(new ShipmentReturnedIntegrationEvent(
                        "evt-1", "ship-1", "order-1", "user-1", "merchant-1", "refused", Instant.now()));

        verify(deliveryOrchestrator).enqueueByEvent(any());
        verify(deliveryOrchestrator).deliverQueuedAsync(any());
    }

    @Test
    void skipsReturnedNotificationWhenMerchantOwnerIsMissing() {
        when(merchantQueryPort.findOwnerIdByMerchantId("merchant-1"))
                .thenReturn(Optional.empty());

        new ShippingEventListener(deliveryOrchestrator, orderQueryPort, merchantQueryPort)
                .on(new ShipmentReturnedIntegrationEvent(
                        "evt-1", "ship-1", "order-1", "user-1", "merchant-1", "refused", Instant.now()));

        verify(deliveryOrchestrator, never()).enqueueByEvent(any());
    }
}
