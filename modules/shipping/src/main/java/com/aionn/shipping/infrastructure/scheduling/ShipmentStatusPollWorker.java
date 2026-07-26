package com.aionn.shipping.infrastructure.scheduling;

import com.aionn.shipping.application.port.out.CarrierClient;
import com.aionn.shipping.application.port.out.ShipmentPersistencePort;
import com.aionn.shipping.domain.model.Shipment;
import com.aionn.shipping.domain.valueobject.ShipmentStatus;
import com.aionn.shipping.infrastructure.carrier.GhnStatusMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShipmentStatusPollWorker {

    private final ShipmentPersistencePort shipmentRepository;
    private final CarrierClient carrierClient;
    private final GhnStatusMapper statusMapper;
    private final ShipmentStatusApplier shipmentStatusApplier;

    public void syncOne(String shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId).orElse(null);
        if (shipment == null || shipment.getTrackingCode() == null
                || shipment.getStatus().isTerminal()) {
            return;
        }
        CarrierClient.OrderDetail detail;
        try {
            detail = carrierClient.fetchOrderDetail(shipment.getTrackingCode());
        } catch (RuntimeException ex) {
            log.warn("GHN detail failed for {} ({}): {}", shipment.getShipmentId(),
                    shipment.getTrackingCode(), ex.getMessage());
            return;
        }
        Optional<ShipmentStatus> mapped = statusMapper.map(detail.status());
        if (mapped.isEmpty()) {
            return;
        }
        try {
            shipmentStatusApplier.apply(shipmentId, mapped.get(), detail);
        } catch (RuntimeException ex) {
            log.warn("Failed to apply polled status for {}: {}", shipmentId, ex.getMessage());
        }
    }
}
