package com.aionn.shipping.infrastructure.integration.listener;

import com.aionn.shipping.application.port.out.shipment.ShipmentRepositoryPort;
import com.aionn.shipping.application.service.ShipmentService;
import com.aionn.shipping.domain.model.Shipment;
import com.aionn.sharedkernel.integration.event.ordering.OrderCancelledIntegrationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderLifecycleListenerTest {

    @Mock
    private ShipmentRepositoryPort shipmentRepository;

    @Mock
    private ShipmentService shipmentService;

    @Mock
    private Shipment shipment1;

    @Mock
    private Shipment shipment2;

    private OrderLifecycleListener listener;

    @BeforeEach
    void setUp() {
        listener = new OrderLifecycleListener(shipmentRepository, shipmentService);
    }

    @Test
    void onOrderCancelledCancelsCancellableShipments() {
        OrderCancelledIntegrationEvent event = new OrderCancelledIntegrationEvent(
                "evt-1",
                "O_1",
                "REASON_1",
                "System cancellation",
                OrderCancelledIntegrationEvent.CancellationType.AUTO_CANCELLED,
                java.time.Instant.now()
        );

        when(shipmentRepository.findByOrderId("O_1")).thenReturn(List.of(shipment1, shipment2));
        when(shipment1.isCancellable()).thenReturn(true);
        when(shipment1.getShipmentId()).thenReturn("S_1");
        when(shipment2.isCancellable()).thenReturn(false);

        listener.onOrderCancelled(event);

        verify(shipmentService).applyCancel("S_1", "AUTO_CANCELLED:REASON_1");
        verify(shipmentService, never()).applyCancel(eq("S_2"), anyString());
    }

    @Test
    void onOrderCancelledHandlesExceptionGracefully() {
        OrderCancelledIntegrationEvent event = new OrderCancelledIntegrationEvent(
                "evt-1",
                "O_1",
                "REASON_1",
                "Customer cancellation",
                OrderCancelledIntegrationEvent.CancellationType.USER_CANCELLED,
                java.time.Instant.now()
        );

        when(shipmentRepository.findByOrderId("O_1")).thenReturn(List.of(shipment1));
        when(shipment1.isCancellable()).thenReturn(true);
        when(shipment1.getShipmentId()).thenReturn("S_1");
        doThrow(new RuntimeException("Error")).when(shipmentService).applyCancel("S_1", "USER_CANCELLED:REASON_1");

        listener.onOrderCancelled(event);

        verify(shipmentService).applyCancel("S_1", "USER_CANCELLED:REASON_1");
    }
}
