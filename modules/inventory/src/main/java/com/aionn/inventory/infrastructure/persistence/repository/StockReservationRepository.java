package com.aionn.inventory.infrastructure.persistence.repository;

import com.aionn.inventory.infrastructure.persistence.entity.StockReservationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import java.time.Instant;
import java.util.List;

public interface StockReservationRepository extends JpaRepository<StockReservationEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM StockReservationEntity r WHERE r.reservationId = :reservationId")
    Optional<StockReservationEntity> lockForUpdate(String reservationId);

    @Query("""
            SELECT r FROM StockReservationEntity r
              WHERE r.orderId = :orderId
                AND r.skuId = :skuId
                AND r.warehouseId = :warehouseId
                AND r.status = 'RESERVED'
            ORDER BY r.reservedAt DESC
            """)
    List<StockReservationEntity> findActiveReservation(
            String orderId, String skuId, String warehouseId, Pageable pageable);

    @Query("""
            SELECT r FROM StockReservationEntity r
              WHERE r.status = 'RESERVED'
                AND r.expiresAt IS NOT NULL
                AND r.expiresAt <= :now
            ORDER BY r.expiresAt ASC
            """)
    List<StockReservationEntity> findExpired(Instant now, Pageable pageable);
}

