package com.aionn.payment.infrastructure.persistence.repository;

import com.aionn.payment.infrastructure.persistence.entity.RefundOperationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface RefundOperationRepository extends JpaRepository<RefundOperationEntity, String> {

    @Query("""
            select coalesce(sum(operation.amount), 0)
            from RefundOperationEntity operation
            where operation.paymentId = :paymentId
              and operation.status = 'PENDING'
            """)
    BigDecimal sumReservedAmount(@Param("paymentId") String paymentId);
}
