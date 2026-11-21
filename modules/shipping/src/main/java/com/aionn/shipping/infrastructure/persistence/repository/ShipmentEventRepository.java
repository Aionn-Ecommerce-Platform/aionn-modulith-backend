package com.aionn.shipping.infrastructure.persistence.repository;

import com.aionn.shipping.infrastructure.persistence.entity.ShipmentEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipmentEventRepository extends JpaRepository<ShipmentEventEntity, String> {

    List<ShipmentEventEntity> findByShipmentIdOrderByOccurredAtAsc(String shipmentId);
}
