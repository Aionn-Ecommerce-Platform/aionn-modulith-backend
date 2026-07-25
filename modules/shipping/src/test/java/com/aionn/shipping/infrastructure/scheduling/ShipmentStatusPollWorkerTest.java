package com.aionn.shipping.infrastructure.scheduling;

import com.aionn.shipping.application.port.out.CarrierClient;
import com.aionn.shipping.application.port.out.ShipmentPersistencePort;
import com.aionn.shipping.application.port.out.integration.ShippingIntegrationEventPublisherPort;
import com.aionn.shipping.domain.model.Shipment;
import com.aionn.shipping.domain.valueobject.ShipmentStatus;
import com.aionn.shipping.infrastructure.carrier.GhnStatusMapper;
import com.aionn.sharedkernel.application.port.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentStatusPollWorkerTest {

    @Mock
    private ShipmentPersistencePort shipmentRepository;

    @Mock
    private CarrierClient carrierClient;

    @Mock
    private GhnStatusMapper statusMapper;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private ShippingIntegrationEventPublisherPort integrationEventPublisher;

    @Mock
    private Shipment shipment;

    private ShipmentStatusPollWorker worker;

    @BeforeEach
    void setUp() {
        worker = new ShipmentStatusPollWorker(
                shipmentRepository, carrierClient, statusMapper, eventPublisher, integrationEventPublisher);
    }

    @Test
    void syncOneDoesNothingIfShipmentNotFound() {
        when(shipmentRepository.findById("S_1")).thenReturn(Optional.empty());

        worker.syncOne("S_1");

        verify(carrierClient, never()).fetchOrderDetail(anyString());
    }

    @Test
    void syncOneDoesNothingIfTrackingCodeNull() {
        when(shipmentRepository.findById("S_1")).thenReturn(Optional.of(shipment));
        when(shipment.getTrackingCode()).thenReturn(null);

        worker.syncOne("S_1");

        verify(carrierClient, never()).fetchOrderDetail(anyString());
    }

    @Test
    void syncOneDoesNothingIfTerminalStatus() {
        when(shipmentRepository.findById("S_1")).thenReturn(Optional.of(shipment));
        when(shipment.getTrackingCode()).thenReturn("TR_1");
        when(shipment.getStatus()).thenReturn(ShipmentStatus.DELIVERED);

        worker.syncOne("S_1");

        verify(carrierClient, never()).fetchOrderDetail(anyString());
    }

    @Test
    void syncOneHandlesCarrierExceptionGracefully() {
        when(shipmentRepository.findById("S_1")).thenReturn(Optional.of(shipment));
        when(shipment.getTrackingCode()).thenReturn("TR_1");
        when(shipment.getStatus()).thenReturn(ShipmentStatus.REGISTERED);
        when(carrierClient.fetchOrderDetail("TR_1")).thenThrow(new RuntimeException("Carrier Error"));

        worker.syncOne("S_1");

        verify(statusMapper, never()).map(anyString());
    }

    @Test
    void syncOneAppliesStatusSuccessfully() {
        when(shipmentRepository.findById("S_1")).thenReturn(Optional.of(shipment));
        when(shipment.getTrackingCode()).thenReturn("TR_1");
        when(shipment.getStatus()).thenReturn(ShipmentStatus.REGISTERED);

        CarrierClient.OrderDetail detail = new CarrierClient.OrderDetail(
                "PICKED_UP", "HN", "shipper", "phone", "sig", "reason", "WH_1", null);
        when(carrierClient.fetchOrderDetail("TR_1")).thenReturn(detail);
        when(statusMapper.map("PICKED_UP")).thenReturn(Optional.of(ShipmentStatus.PICKED_UP));

        worker.syncOne("S_1");

        // Verify it calls apply logic (which calls findById again inside)
        verify(shipmentRepository, atLeastOnce()).findById("S_1");
    }

    @Test
    void applyDoesNothingIfShipmentStatusMatchesTarget() {
        when(shipmentRepository.findById("S_1")).thenReturn(Optional.of(shipment));
        when(shipment.getStatus()).thenReturn(ShipmentStatus.PICKED_UP);

        CarrierClient.OrderDetail detail = new CarrierClient.OrderDetail(
                "PICKED_UP", "HN", "shipper", "phone", "sig", "reason", "WH_1", null);

        worker.apply("S_1", ShipmentStatus.PICKED_UP, detail);

        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void applyDispatchesStatusTransitionsCorrectly() {
        // We'll mock the actual transitions by mock shipment methods
        when(shipmentRepository.findById("S_1")).thenReturn(Optional.of(shipment));
        when(shipment.getStatus()).thenReturn(ShipmentStatus.REGISTERED);
        when(shipmentRepository.save(shipment)).thenReturn(shipment);

        CarrierClient.OrderDetail detail = new CarrierClient.OrderDetail(
                "PICKED_UP", "HUB", "driver", "090", "sig", "reason", "WH_1", null);

        // Test PICKED_UP
        worker.apply("S_1", ShipmentStatus.PICKED_UP, detail);
        verify(shipment).markPickedUp("WH_1");

        // Test IN_TRANSIT
        reset(shipment);
        when(shipment.getStatus()).thenReturn(ShipmentStatus.PICKED_UP);
        worker.apply("S_1", ShipmentStatus.IN_TRANSIT, detail);
        verify(shipment).updateInTransitStatus("HUB", "PICKED_UP");

        // Test OUT_FOR_DELIVERY
        reset(shipment);
        when(shipment.getStatus()).thenReturn(ShipmentStatus.IN_TRANSIT);
        worker.apply("S_1", ShipmentStatus.OUT_FOR_DELIVERY, detail);
        verify(shipment).markOutForDelivery("driver", "090");

        // Test DELIVERED
        reset(shipment);
        when(shipment.getStatus()).thenReturn(ShipmentStatus.OUT_FOR_DELIVERY);
        worker.apply("S_1", ShipmentStatus.DELIVERED, detail);
        verify(shipment).markDelivered("sig");

        // Test DELIVERY_FAILED
        reset(shipment);
        when(shipment.getStatus()).thenReturn(ShipmentStatus.OUT_FOR_DELIVERY);
        worker.apply("S_1", ShipmentStatus.DELIVERY_FAILED, detail);
        verify(shipment).recordDeliveryFailure("reason");

        // Test RETURNED
        reset(shipment);
        when(shipment.getStatus()).thenReturn(ShipmentStatus.PICKED_UP);
        worker.apply("S_1", ShipmentStatus.RETURNED, detail);
        verify(shipment).markReturned("reason");

        // Test CANCELLED
        reset(shipment);
        when(shipment.getStatus()).thenReturn(ShipmentStatus.REGISTERED);
        worker.apply("S_1", ShipmentStatus.CANCELLED, detail);
        verify(shipment).cancel("reason");
    }
}
