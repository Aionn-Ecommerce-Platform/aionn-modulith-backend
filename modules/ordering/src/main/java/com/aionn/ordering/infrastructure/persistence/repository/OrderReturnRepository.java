package com.aionn.ordering.infrastructure.persistence.repository;

import com.aionn.ordering.infrastructure.persistence.entity.OrderReturnEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface OrderReturnRepository extends JpaRepository<OrderReturnEntity, String> {

    List<OrderReturnEntity> findByStatusOrderByRequestedAtDesc(String status, Pageable pageable);

    List<OrderReturnEntity> findByUserIdOrderByRequestedAtDesc(String userId, Pageable pageable);

    List<OrderReturnEntity> findByMerchantIdOrderByRequestedAtDesc(String merchantId, Pageable pageable);

    @Query("""
        SELECT COUNT(r) AS requests,
               COALESCE(SUM(CASE WHEN r.status IN ('APPROVED', 'ITEM_RECEIVED', 'REFUNDED') THEN 1 ELSE 0 END), 0) AS approved,
               COALESCE(SUM(CASE WHEN r.status IN ('APPROVED', 'ITEM_RECEIVED', 'REFUNDED')
                                 THEN COALESCE(r.refundAmount, 0) ELSE 0 END), 0) AS refundTotal
          FROM OrderReturnEntity r
         WHERE r.merchantId = :merchantId
           AND (:from IS NULL OR r.requestedAt >= :from)
           AND (:to IS NULL OR r.requestedAt < :to)
        """)
    MerchantReturnSummaryProjection summarizeByMerchant(
            @Param("merchantId") String merchantId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    boolean existsByOrderIdAndStatusIn(String orderId, java.util.Collection<String> statuses);

    @Query("""
        SELECT r.returnId FROM OrderReturnEntity r
         WHERE r.refundStatus IN ('PENDING', 'FAILED')
           AND r.refundAttempts < :maxAttempts
           AND (r.nextRefundAttemptAt IS NULL OR r.nextRefundAttemptAt <= :now)
         ORDER BY r.nextRefundAttemptAt ASC, r.returnId ASC
        """)
    List<String> findRefundRetryIds(Instant now, int maxAttempts, Pageable pageable);

    @Query("""
        SELECT r.status AS status,
               r.reasonType AS reasonType,
               r.reason AS reason,
               r.refundAmount AS refundAmount,
               r.refundCurrency AS refundCurrency
          FROM OrderReturnEntity r
         WHERE r.requestedAt >= :from
           AND r.requestedAt < :to
        """)
    List<ReturnAnalyticsProjection> findReturnAnalyticsRows(Instant from, Instant to);

    @Query("""
        SELECT COUNT(o) FROM OrderEntity o
          WHERE o.status = 'COMPLETED'
            AND o.completedAt >= :from
            AND o.completedAt < :to
        """)
    long countCompletedOrdersBetween(Instant from, Instant to);

    interface ReturnAnalyticsProjection {
        String getStatus();

        String getReasonType();

        String getReason();

        BigDecimal getRefundAmount();

        String getRefundCurrency();
    }

    interface MerchantReturnSummaryProjection {
        Long getRequests();

        Long getApproved();

        BigDecimal getRefundTotal();
    }
}
