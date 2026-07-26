package com.aionn.promotion.infrastructure.persistence.repository;

import com.aionn.promotion.infrastructure.persistence.entity.UserVoucherEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserVoucherRepository extends JpaRepository<UserVoucherEntity, String> {

    Optional<UserVoucherEntity> findByUserIdAndVoucherCode(String userId, String voucherCode);

    Optional<UserVoucherEntity> findByReservedOrderId(String reservedOrderId);

    @Query("""
            SELECT COUNT(uv) FROM UserVoucherEntity uv
              JOIN VoucherEntity v ON uv.voucherCode = v.voucherCode
              WHERE uv.userId = :userId AND v.campaignId = :campaignId
            """)
    long countByUserAndCampaign(String userId, String campaignId);

    List<UserVoucherEntity> findByUserIdOrderByClaimedAtDesc(String userId, Pageable pageable);

    @Query("""
            SELECT uv FROM UserVoucherEntity uv
              WHERE uv.status = 'RESERVED'
                AND uv.reservedExpiresAt IS NOT NULL
                AND uv.reservedExpiresAt <= :now
            """)
    List<UserVoucherEntity> findExpiredReservations(Instant now, Pageable pageable);
}

