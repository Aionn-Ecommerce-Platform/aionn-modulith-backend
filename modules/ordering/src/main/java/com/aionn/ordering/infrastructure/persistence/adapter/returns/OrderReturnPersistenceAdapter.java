package com.aionn.ordering.infrastructure.persistence.adapter.returns;

import java.time.Instant;

import com.aionn.ordering.application.port.out.OrderReturnPersistencePort;
import com.aionn.ordering.domain.model.OrderReturn;
import com.aionn.ordering.domain.valueobject.ReturnStatus;
import com.aionn.ordering.infrastructure.persistence.entity.OrderReturnEntity;
import com.aionn.ordering.infrastructure.persistence.mapper.OrderReturnDomainMapper;
import com.aionn.ordering.infrastructure.persistence.repository.OrderReturnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderReturnPersistenceAdapter implements OrderReturnPersistencePort {

    private final OrderReturnRepository jpa;
    private final OrderReturnDomainMapper mapper;

    @Override
    public OrderReturn save(OrderReturn r) {
        OrderReturnEntity existing = jpa.findById(r.getReturnId()).orElse(null);
        OrderReturnEntity entity = mapper.toEntity(r, existing);
        return mapper.toDomain(jpa.saveAndFlush(entity));
    }

    @Override
    public Optional<OrderReturn> findById(String returnId) {
        return jpa.findById(returnId).map(mapper::toDomain);
    }

    @Override
    public List<OrderReturn> findByStatus(ReturnStatus status, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return jpa.findByStatusOrderByRequestedAtDesc(status.name(), PageRequest.of(0, safeLimit))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<OrderReturn> findByUserId(String userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return jpa.findByUserIdOrderByRequestedAtDesc(userId, PageRequest.of(0, safeLimit))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<OrderReturn> findByMerchantId(String merchantId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return jpa.findByMerchantIdOrderByRequestedAtDesc(merchantId, PageRequest.of(0, safeLimit))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<OrderReturn> findByMerchantId(String merchantId, int page, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return jpa.findByMerchantIdOrderByRequestedAtDesc(
                        merchantId,
                        PageRequest.of(Math.max(0, page), safeLimit))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByMerchantId(String merchantId) {
        return jpa.countByMerchantId(merchantId);
    }

    @Override
    public List<OrderReturn> searchByMerchant(
            String merchantId,
            String status,
            String reasonType,
            String search,
            int page,
            int limit) {
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim().toLowerCase();
        boolean hasSearch = normalizedSearch != null;
        String pattern = hasSearch ? "%" + normalizedSearch + "%" : "";
        String normalizedStatus = normalize(status);
        String normalizedReason = normalize(reasonType);
        return jpa.searchMerchantReturns(
                        merchantId,
                        normalizedStatus != null,
                        normalizedStatus == null ? "" : normalizedStatus,
                        normalizedReason != null,
                        normalizedReason == null ? "" : normalizedReason,
                        hasSearch,
                        pattern,
                        PageRequest.of(Math.max(0, page), Math.max(1, Math.min(limit, 100))))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countSearchByMerchant(
            String merchantId,
            String status,
            String reasonType,
            String search) {
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim().toLowerCase();
        boolean hasSearch = normalizedSearch != null;
        String pattern = hasSearch ? "%" + normalizedSearch + "%" : "";
        String normalizedStatus = normalize(status);
        String normalizedReason = normalize(reasonType);
        return jpa.countSearchMerchantReturns(
                merchantId,
                normalizedStatus != null,
                normalizedStatus == null ? "" : normalizedStatus,
                normalizedReason != null,
                normalizedReason == null ? "" : normalizedReason,
                hasSearch,
                pattern);
    }

    @Override
    public List<ReturnStatusCount> countByMerchantGroupByStatus(String merchantId) {
        return jpa.countMerchantReturnsByStatus(merchantId).stream()
                .map(row -> new ReturnStatusCount(row.getStatus(), row.getCount()))
                .toList();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Override
    public boolean hasActiveReturn(String orderId) {
        return jpa.existsByOrderIdAndStatusIn(orderId,
                List.of("REQUESTED", "APPROVED", "ITEM_RECEIVED"));
    }

    @Override
    public List<String> findRefundRetryIds(Instant now, int maxAttempts, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return jpa.findRefundRetryIds(now, maxAttempts, PageRequest.of(0, safeLimit));
    }

    @Override
    public List<ReturnAnalyticsRow> findReturnAnalyticsRows(Instant from, Instant to) {
        return jpa.findReturnAnalyticsRows(from, to).stream()
                .map(row -> new ReturnAnalyticsRow(
                        row.getStatus(),
                        row.getReasonType(),
                        row.getReason(),
                        row.getRefundAmount(),
                        row.getRefundCurrency()))
                .toList();
    }

    @Override
    public ReturnSummary summarizeByMerchant(String merchantId, Instant from, Instant to) {
        var row = jpa.summarizeByMerchant(merchantId, from, to);
        return new ReturnSummary(
                row == null || row.getRequests() == null ? 0L : row.getRequests(),
                row == null || row.getApproved() == null ? 0L : row.getApproved(),
                row == null || row.getRefundTotal() == null
                        ? java.math.BigDecimal.ZERO
                        : row.getRefundTotal());
    }

    @Override
    public long countCompletedOrdersBetween(Instant from, Instant to) {
        return jpa.countCompletedOrdersBetween(from, to);
    }
}

