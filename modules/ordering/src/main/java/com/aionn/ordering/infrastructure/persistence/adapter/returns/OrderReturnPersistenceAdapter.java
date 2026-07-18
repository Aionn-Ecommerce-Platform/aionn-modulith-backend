package com.aionn.ordering.infrastructure.persistence.adapter.returns;

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
        return mapper.toDomain(jpa.save(entity));
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
    public List<ReturnAnalyticsRow> findReturnAnalyticsRows(java.time.Instant from, java.time.Instant to) {
        return jpa.findReturnAnalyticsRows(from, to).stream()
                .map(row -> new ReturnAnalyticsRow(
                        row.getStatus(),
                        row.getReason(),
                        row.getRefundAmount(),
                        row.getRefundCurrency()))
                .toList();
    }

    @Override
    public long countCompletedOrdersBetween(java.time.Instant from, java.time.Instant to) {
        return jpa.countCompletedOrdersBetween(from, to);
    }
}

