package com.aionn.ordering.infrastructure.persistence.repository;

import com.aionn.ordering.infrastructure.persistence.entity.OrderReturnEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface OrderReturnRepository extends JpaRepository<OrderReturnEntity, String> {

    List<OrderReturnEntity> findByStatusOrderByRequestedAtDesc(String status, Pageable pageable);

    List<OrderReturnEntity> findByUserIdOrderByRequestedAtDesc(String userId, Pageable pageable);

    List<OrderReturnEntity> findByMerchantIdOrderByRequestedAtDesc(String merchantId, Pageable pageable);

    @Query("""
        SELECT r.status AS status,
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

        String getReason();

        BigDecimal getRefundAmount();

        String getRefundCurrency();
    }
}
