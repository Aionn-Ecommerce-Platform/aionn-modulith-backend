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

    List<PaymentEntity> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndCurrency(
            Instant fromInclusive, Instant toExclusive, String currency);
}

