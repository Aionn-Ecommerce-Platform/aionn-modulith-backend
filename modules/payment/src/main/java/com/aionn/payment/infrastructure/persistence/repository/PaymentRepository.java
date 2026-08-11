package com.aionn.payment.infrastructure.persistence.repository;

import com.aionn.payment.infrastructure.persistence.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from PaymentEntity payment where payment.paymentId = :paymentId")
    Optional<PaymentEntity> lockById(@Param("paymentId") String paymentId);

    Optional<PaymentEntity> findByIdempotencyKey(String idempotencyKey);

    List<PaymentEntity> findByOrderId(String orderId);

    @Query("SELECT COALESCE(SUM(p.refundedAmount), 0) AS refundAmount, "
            + "SUM(CASE WHEN p.refundedAmount > 0 THEN 1 ELSE 0 END) AS refundCount, "
            + "SUM(CASE WHEN p.status IN ('PAID', 'REFUNDED') THEN 1 ELSE 0 END) AS paidCount "
            + "FROM PaymentEntity p WHERE p.createdAt >= :from AND p.createdAt < :to AND p.currency = :currency")
    PaymentSummaryProjection summarize(@Param("from") Instant from, @Param("to") Instant to,
            @Param("currency") String currency);

    @Query("SELECT p.status AS status, COUNT(p) AS count FROM PaymentEntity p "
            + "WHERE p.createdAt >= :from AND p.createdAt < :to AND p.currency = :currency GROUP BY p.status")
    List<StatusCountProjection> countByStatus(@Param("from") Instant from, @Param("to") Instant to,
            @Param("currency") String currency);

    interface PaymentSummaryProjection {
        java.math.BigDecimal getRefundAmount();
        Long getRefundCount();
        Long getPaidCount();
    }

    interface StatusCountProjection {
        String getStatus();
        Long getCount();
    }
}

