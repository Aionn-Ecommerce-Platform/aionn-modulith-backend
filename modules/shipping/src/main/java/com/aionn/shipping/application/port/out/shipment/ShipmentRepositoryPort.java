package com.aionn.shipping.application.port.out.shipment;

import com.aionn.shipping.domain.model.Shipment;

import java.util.List;
import java.util.Optional;

public interface ShipmentRepositoryPort {

    Shipment save(Shipment shipment);

    Optional<Shipment> findById(String shipmentId);

    Optional<Shipment> findByTrackingCode(String trackingCode);

    List<Shipment> findByOrderId(String orderId);

    List<Shipment> findByStatus(String status, int limit);
}
