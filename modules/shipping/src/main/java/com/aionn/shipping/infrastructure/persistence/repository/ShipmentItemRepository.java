package com.aionn.shipping.infrastructure.persistence.repository;

import com.aionn.shipping.infrastructure.persistence.entity.ShipmentItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShipmentItemRepository extends JpaRepository<ShipmentItemEntity, ShipmentItemEntity.ShipmentItemId> {

    @Query("""
            SELECT si.id.skuId, SUM(si.qty), MAX(s.deliveredAt)
              FROM ShipmentItemEntity si
              JOIN ShipmentEntity s ON s.shipmentId = si.id.shipmentId
             WHERE si.orderId = :orderId
               AND s.status = 'DELIVERED'
             GROUP BY si.id.skuId
            """)
    List<Object[]> findDeliveredByOrderId(@Param("orderId") String orderId);
}
