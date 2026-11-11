package com.aionn.shipping.application.port.out;

import com.aionn.shipping.domain.model.Shipment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ShipmentPersistencePort {

    Shipment save(Shipment shipment);

    Optional<Shipment> findById(String shipmentId);

    /** Row-locked read used to serialize carrier registration for one shipment. */
    Optional<Shipment> findByIdForUpdate(String shipmentId);

    Optional<Shipment> findByTrackingCode(String trackingCode);

    List<Shipment> findByOrderId(String orderId);

    /** Records a polling attempt so the same shipments are not retried forever. */
    void markPolled(String shipmentId, Instant polledAt);

    /**
     * Shipments that still need carrier polling (have a tracking code and are
     * not yet in a terminal state), least recently polled first.
     */
    List<Shipment> findActiveTracking(int batchSize);
}
