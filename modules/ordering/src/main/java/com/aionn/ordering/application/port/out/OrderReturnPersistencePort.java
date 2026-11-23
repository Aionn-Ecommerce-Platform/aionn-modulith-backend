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

    List<OrderReturn> findByMerchantId(String merchantId, int page, int limit);

    long countByMerchantId(String merchantId);

    List<OrderReturn> searchByMerchant(
            String merchantId,
            String status,
            String reasonType,
            String search,
            int page,
            int limit);

    long countSearchByMerchant(
            String merchantId,
            String status,
            String reasonType,
            String search);

    List<ReturnStatusCount> countByMerchantGroupByStatus(String merchantId);

    record ReturnStatusCount(String status, long count) {
    }

    boolean hasActiveReturn(String orderId);

    List<String> findRefundRetryIds(Instant now, int maxAttempts, int limit);

    List<ReturnAnalyticsRow> findReturnAnalyticsRows(Instant from, Instant to);

    ReturnSummary summarizeByMerchant(String merchantId, Instant from, Instant to);

    long countCompletedOrdersBetween(Instant from, Instant to);

    record ReturnSummary(long requests, long approved, BigDecimal refundTotal) {
    }

    record ReturnAnalyticsRow(
            String status,
            String reasonType,
            String reason,
            BigDecimal refundAmount,
            String refundCurrency) {
    }
}

