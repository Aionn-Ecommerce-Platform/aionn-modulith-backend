package com.aionn.shipping.infrastructure.persistence.repository;

import com.aionn.shipping.infrastructure.persistence.entity.ShipmentEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<ShipmentEntity, String> {

    Optional<ShipmentEntity> findByTrackingCode(String trackingCode);

    List<ShipmentEntity> findByOrderId(String orderId);

    List<ShipmentEntity> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    List<ShipmentEntity> findByTrackingCodeIsNotNullAndStatusNotIn(
            Collection<String> terminalStatuses, Pageable pageable);
}
