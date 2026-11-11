package com.aionn.shipping.infrastructure.persistence.repository;

import java.time.Instant;

import com.aionn.shipping.infrastructure.persistence.entity.ShipmentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<ShipmentEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ShipmentEntity s WHERE s.shipmentId = :shipmentId")
    Optional<ShipmentEntity> findByIdForUpdate(@Param("shipmentId") String shipmentId);

    Optional<ShipmentEntity> findByTrackingCode(String trackingCode);

    List<ShipmentEntity> findByOrderId(String orderId);

    List<ShipmentEntity> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    @Query("""
            SELECT s FROM ShipmentEntity s
             WHERE s.trackingCode IS NOT NULL AND s.status NOT IN :terminalStatuses
             ORDER BY s.lastPolledAt ASC NULLS FIRST
            """)
    List<ShipmentEntity> findActiveTracking(@Param("terminalStatuses") Collection<String> terminalStatuses,
            Pageable pageable);

    @Modifying
    @Query("UPDATE ShipmentEntity s SET s.lastPolledAt = :polledAt WHERE s.shipmentId = :shipmentId")
    int updateLastPolledAt(@Param("shipmentId") String shipmentId, @Param("polledAt") Instant polledAt);
}
