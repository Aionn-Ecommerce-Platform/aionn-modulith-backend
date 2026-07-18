package com.aionn.ordering.application.port.out;

import com.aionn.ordering.domain.model.OrderReturn;
import com.aionn.ordering.domain.valueobject.ReturnStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderReturnPersistencePort {

    OrderReturn save(OrderReturn orderReturn);

    Optional<OrderReturn> findById(String returnId);

    List<OrderReturn> findByStatus(ReturnStatus status, int limit);

    List<OrderReturn> findByUserId(String userId, int limit);

    List<OrderReturn> findByMerchantId(String merchantId, int limit);

    List<ReturnAnalyticsRow> findReturnAnalyticsRows(Instant from, Instant to);

    long countCompletedOrdersBetween(Instant from, Instant to);

    record ReturnAnalyticsRow(
            String status,
            String reason,
            BigDecimal refundAmount,
            String refundCurrency) {
    }
}

