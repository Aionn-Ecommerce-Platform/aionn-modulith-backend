package com.aionn.payment.application.port.out;

import com.aionn.payment.domain.model.RefundOperation;

import java.math.BigDecimal;
import java.util.Optional;

public interface RefundOperationPersistencePort {

    Optional<RefundOperation> findByIdempotencyKey(String idempotencyKey);

    BigDecimal sumReservedAmount(String paymentId);

    RefundOperation save(RefundOperation operation);
}
