package com.aionn.shipping.infrastructure.scheduling;

import com.aionn.shipping.application.port.out.CarrierClient;
import com.aionn.shipping.application.port.out.ShipmentPersistencePort;
import com.aionn.shipping.domain.model.Shipment;
import com.aionn.shipping.domain.valueobject.ShipmentStatus;
import com.aionn.shipping.infrastructure.carrier.GhnStatusMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentStatusPollWorkerTest {

    @Mock
    private ShipmentPersistencePort shipmentRepository;

    @Mock
    private CarrierClient carrierClient;

    @Mock
    private GhnStatusMapper statusMapper;

    @Mock
    private ShipmentStatusApplier shipmentStatusApplier;

    @Mock
    private Shipment shipment;

    @InjectMocks
    private ShipmentStatusPollWorker worker;

    private static CarrierClient.OrderDetail detail(String status) {
        return new CarrierClient.OrderDetail(
                status, "HN", "shipper", "phone", "sig", "reason", "WH_1", null);
    }

    @Test
    void syncOneDoesNothingIfShipmentNotFound() {
        when(shipmentRepository.findById("S_1")).thenReturn(Optional.empty());

        worker.syncOne("S_1");

        verifyNoInteractions(carrierClient, shipmentStatusApplier);
    }

    @Test
    void syncOneDoesNothingIfTrackingCodeNull() {
        when(shipmentRepository.findById("S_1")).thenReturn(Optional.of(shipment));
        when(shipment.getTrackingCode()).thenReturn(null);

        worker.syncOne("S_1");

        verifyNoInteractions(carrierClient, shipmentStatusApplier);
    }

    @Test
    void syncOneDoesNothingIfTerminalStatus() {
        when(shipmentRepository.findById("S_1")).thenReturn(Optional.of(shipment));
        when(shipment.getTrackingCode()).thenReturn("TR_1");
        when(shipment.getStatus()).thenReturn(ShipmentStatus.DELIVERED);

        worker.syncOne("S_1");

        verifyNoInteractions(carrierClient, shipmentStatusApplier);
    }

    @Test
    void syncOneHandlesCarrierExceptionGracefully() {
        when(shipmentRepository.findById("S_1")).thenReturn(Optional.of(shipment));
        when(shipment.getTrackingCode()).thenReturn("TR_1");
        when(shipment.getStatus()).thenReturn(ShipmentStatus.REGISTERED);
        when(carrierClient.fetchOrderDetail("TR_1")).thenThrow(new RuntimeException("Carrier Error"));

        worker.syncOne("S_1");

        verify(statusMapper, never()).map(anyString());
        verifyNoInteractions(shipmentStatusApplier);
    }

    @Test
    void syncOneSkipsWhenCarrierStatusIsUnmappable() {
        when(shipmentRepository.findById("S_1")).thenReturn(Optional.of(shipment));
        when(shipment.getTrackingCode()).thenReturn("TR_1");
        when(shipment.getStatus()).thenReturn(ShipmentStatus.REGISTERED);
        when(carrierClient.fetchOrderDetail("TR_1")).thenReturn(detail("teleported"));
        when(statusMapper.map("teleported")).thenReturn(Optional.empty());

        worker.syncOne("S_1");

        verifyNoInteractions(shipmentStatusApplier);
    }

    @Test
    void syncOneDelegatesMappedStatusToTheApplier() {
        when(shipmentRepository.findById("S_1")).thenReturn(Optional.of(shipment));
        when(shipment.getTrackingCode()).thenReturn("TR_1");
        when(shipment.getStatus()).thenReturn(ShipmentStatus.REGISTERED);
        CarrierClient.OrderDetail detail = detail("PICKED_UP");
        when(carrierClient.fetchOrderDetail("TR_1")).thenReturn(detail);
        when(statusMapper.map("PICKED_UP")).thenReturn(Optional.of(ShipmentStatus.PICKED_UP));

        worker.syncOne("S_1");

        verify(shipmentStatusApplier).apply("S_1", ShipmentStatus.PICKED_UP, detail);
    }

    @Test
    void syncOneSwallowsApplierFailure() {
        when(shipmentRepository.findById("S_1")).thenReturn(Optional.of(shipment));
        when(shipment.getTrackingCode()).thenReturn("TR_1");
        when(shipment.getStatus()).thenReturn(ShipmentStatus.REGISTERED);
        when(carrierClient.fetchOrderDetail("TR_1")).thenReturn(detail("PICKED_UP"));
        when(statusMapper.map("PICKED_UP")).thenReturn(Optional.of(ShipmentStatus.PICKED_UP));
        org.mockito.Mockito.doThrow(new RuntimeException("db down"))
                .when(shipmentStatusApplier).apply(anyString(), any(), any());

        worker.syncOne("S_1");

        verify(shipmentStatusApplier).apply(anyString(), any(), any());
    }
}
