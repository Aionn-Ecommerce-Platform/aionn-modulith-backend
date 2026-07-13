package com.aionn.inventory.infrastructure.persistence.repository;

import com.aionn.inventory.infrastructure.persistence.entity.StockReservationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface StockReservationRepository extends JpaRepository<StockReservationEntity, String> {

    @Query("""
            SELECT r FROM StockReservationEntity r
              WHERE r.status = 'RESERVED'
                AND r.expiresAt IS NOT NULL
                AND r.expiresAt <= :now
            ORDER BY r.expiresAt ASC
            """)
    List<StockReservationEntity> findExpired(Instant now, Pageable pageable);
}

