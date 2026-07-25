package com.aionn.shipping.application.port.out;

import com.aionn.shipping.domain.model.Shipment;

import java.util.List;
import java.util.Optional;

public interface ShipmentPersistencePort {

    Shipment save(Shipment shipment);

    Optional<Shipment> findById(String shipmentId);

    Optional<Shipment> findByTrackingCode(String trackingCode);

    List<Shipment> findByOrderId(String orderId);

    /**
     * Shipments that still need carrier polling (have a tracking code and are
     * not yet in a terminal state).
     */
    List<Shipment> findActiveTracking(int batchSize);
}
