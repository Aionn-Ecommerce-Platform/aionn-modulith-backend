package com.aionn.shipping.application.port.out;

import com.aionn.shipping.domain.valueobject.ShipmentTrackingEvent;

import java.time.Instant;
import java.util.List;

public interface ShipmentEventPersistencePort {

    ShipmentTrackingEvent record(String shipmentId, String status, String description, String location,
            Instant occurredAt);

    List<ShipmentTrackingEvent> findByShipmentId(String shipmentId);
}
