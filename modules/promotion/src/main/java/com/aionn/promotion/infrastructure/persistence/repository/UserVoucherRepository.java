package com.aionn.promotion.infrastructure.persistence.repository;

import com.aionn.promotion.infrastructure.persistence.entity.UserVoucherEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserVoucherRepository extends JpaRepository<UserVoucherEntity, String> {

    Optional<UserVoucherEntity> findByUserIdAndVoucherCode(String userId, String voucherCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT uv FROM UserVoucherEntity uv WHERE uv.userVoucherId = :id")
    Optional<UserVoucherEntity> findByIdForUpdate(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT uv FROM UserVoucherEntity uv WHERE uv.userId = :userId AND uv.voucherCode = :voucherCode")
    Optional<UserVoucherEntity> findByUserIdAndVoucherCodeForUpdate(
            @Param("userId") String userId, @Param("voucherCode") String voucherCode);

    Optional<UserVoucherEntity> findByUserIdAndVoucherCodeAndEntitlementKey(
            String userId, String voucherCode, String entitlementKey);

    Optional<UserVoucherEntity> findByReservedOrderId(String reservedOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT uv FROM UserVoucherEntity uv WHERE uv.reservedOrderId = :orderId")
    Optional<UserVoucherEntity> findByReservedOrderIdForUpdate(@Param("orderId") String orderId);

    @Query("""
            SELECT COUNT(uv) FROM UserVoucherEntity uv
              JOIN VoucherEntity v ON uv.voucherCode = v.voucherCode
              WHERE uv.userId = :userId AND v.campaignId = :campaignId
            """)
    long countByUserAndCampaign(String userId, String campaignId);

    @Query("""
            SELECT COUNT(uv) FROM UserVoucherEntity uv
              WHERE uv.userId = :userId
                AND uv.claimedAt >= :since
            """)
    long countByUserIdAndClaimedAtGreaterThanEqual(
            @Param("userId") String userId, @Param("since") Instant since);

    @Query("""
            SELECT COUNT(uv) FROM UserVoucherEntity uv, VoucherEntity v
              WHERE uv.voucherCode = v.voucherCode
                AND uv.userId = :userId
                AND uv.status IN ('CLAIMED', 'RESERVED', 'RELEASED')
                AND (v.validUntil IS NULL OR v.validUntil > :now)
            """)
    long countActiveByUserId(@Param("userId") String userId, @Param("now") Instant now);

    List<UserVoucherEntity> findByUserIdOrderByClaimedAtDesc(String userId, Pageable pageable);

    @Query("""
            SELECT uv FROM UserVoucherEntity uv
             WHERE uv.userId = :userId
               AND (:hasStatuses = false OR uv.status IN :statuses)
             ORDER BY CASE WHEN uv.status IN ('CLAIMED', 'RELEASED') THEN 0
                           WHEN uv.status IN ('APPLIED', 'RESERVED') THEN 1
                           ELSE 2 END,
                      uv.claimedAt DESC
            """)
    List<UserVoucherEntity> searchByUserId(
            @Param("userId") String userId,
            @Param("hasStatuses") boolean hasStatuses,
            @Param("statuses") Collection<String> statuses,
            Pageable pageable);

    @Query("""
            SELECT COUNT(uv) FROM UserVoucherEntity uv
             WHERE uv.userId = :userId
               AND (:hasStatuses = false OR uv.status IN :statuses)
            """)
    long countSearchByUserId(
            @Param("userId") String userId,
            @Param("hasStatuses") boolean hasStatuses,
            @Param("statuses") Collection<String> statuses);

    @Query("""
            SELECT uv FROM UserVoucherEntity uv
              WHERE uv.status = 'RESERVED'
                AND uv.reservedExpiresAt IS NOT NULL
                AND uv.reservedExpiresAt <= :now
            """)
    List<UserVoucherEntity> findExpiredReservations(Instant now, Pageable pageable);
}

