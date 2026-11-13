package com.aionn.shipping.infrastructure.integration.listener;

import java.time.Instant;

import com.aionn.shipping.application.port.out.ShipmentPersistencePort;
import com.aionn.shipping.application.service.ShipmentCarrierOrchestrator;
import com.aionn.shipping.domain.model.Shipment;
import com.aionn.contracts.event.ordering.OrderCancelledIntegrationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class OrderLifecycleListenerTest {

    @Mock
    private ShipmentPersistencePort shipmentPersistencePort;

    @Mock
    private ShipmentCarrierOrchestrator shipmentCarrierOrchestrator;

    @Mock
    private Shipment shipment1;

    @Mock
    private Shipment shipment2;

    private OrderLifecycleListener listener;

    @BeforeEach
    void setUp() {
        listener = new OrderLifecycleListener(shipmentPersistencePort, shipmentCarrierOrchestrator);
    }

    @Test
    void onOrderCancelledCancelsCancellableShipments() {
        OrderCancelledIntegrationEvent event = new OrderCancelledIntegrationEvent(
                "evt-1",
                "O_1",
                "REASON_1",
                "System cancellation",
                OrderCancelledIntegrationEvent.CancellationType.AUTO_CANCELLED,
                Instant.now()
        );

        when(shipmentPersistencePort.findByOrderId("O_1")).thenReturn(List.of(shipment1, shipment2));
        when(shipment1.isCancellable()).thenReturn(true);
        when(shipment1.getShipmentId()).thenReturn("S_1");
        when(shipment2.isCancellable()).thenReturn(false);

        listener.onOrderCancelled(event);

        verify(shipmentCarrierOrchestrator).cancelShipmentSystem("S_1", "AUTO_CANCELLED:REASON_1");
        verify(shipmentCarrierOrchestrator, never()).cancelShipmentSystem(eq("S_2"), anyString());
    }

    @Test
    void onOrderCancelledPropagatesFailureForOutboxRetry() {
        OrderCancelledIntegrationEvent event = new OrderCancelledIntegrationEvent(
                "evt-1",
                "O_1",
                "REASON_1",
                "Customer cancellation",
                OrderCancelledIntegrationEvent.CancellationType.USER_CANCELLED,
                Instant.now()
        );

        when(shipmentPersistencePort.findByOrderId("O_1")).thenReturn(List.of(shipment1));
        when(shipment1.isCancellable()).thenReturn(true);
        when(shipment1.getShipmentId()).thenReturn("S_1");
        doThrow(new RuntimeException("Error")).when(shipmentCarrierOrchestrator)
                .cancelShipmentSystem("S_1", "USER_CANCELLED:REASON_1");

        assertThrows(IllegalStateException.class, () -> listener.onOrderCancelled(event));

        verify(shipmentCarrierOrchestrator).cancelShipmentSystem("S_1", "USER_CANCELLED:REASON_1");
    }
}
