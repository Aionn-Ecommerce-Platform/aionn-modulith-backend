package com.aionn.ordering.infrastructure.persistence.adapter.order;

import com.aionn.ordering.application.port.out.OrderPersistencePort;
import com.aionn.ordering.domain.model.Order;
import com.aionn.ordering.infrastructure.persistence.entity.OrderEntity;
import com.aionn.ordering.infrastructure.persistence.mapper.OrderDomainMapper;
import com.aionn.ordering.infrastructure.persistence.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderPersistenceAdapter implements OrderPersistencePort {

    private final OrderRepository jpa;
    private final OrderDomainMapper mapper;

    @Override
    public Order save(Order order) {
        OrderEntity existing = jpa.findById(order.getOrderId()).orElse(null);
        OrderEntity entity = mapper.toEntity(order, existing);
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<Order> findById(String orderId) {
        return jpa.findById(orderId).map(mapper::toDomain);
    }

    @Override
    public List<Order> findByUser(String userId, int limit) {
        return jpa.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, Math.max(1, limit))).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Order> findByUserAndStatuses(String userId, List<String> statuses, int limit) {
        return jpa.findByUserIdAndStatusInOrderByCreatedAtDesc(
                        userId,
                        statuses,
                        PageRequest.of(0, Math.max(1, limit)))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Order> findByMerchant(String merchantId, int limit) {
        return jpa.findByMerchantIdOrderByCreatedAtDesc(merchantId, PageRequest.of(0, Math.max(1, limit))).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Order> findByMerchantAndStatuses(String merchantId, List<String> statuses, int limit) {
        return jpa.findByMerchantIdAndStatusInOrderByCreatedAtDesc(
                        merchantId,
                        statuses,
                        PageRequest.of(0, Math.max(1, limit)))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<OrderAnalyticsRow> findMerchantAnalyticsRows(String merchantId, Instant from, Instant to) {
        return jpa.findMerchantAnalyticsRows(merchantId, from, to).stream()
                .map(row -> new OrderAnalyticsRow(
                        row.getStatus(),
                        row.getTotalAmount(),
                        row.getCurrency(),
                        row.getCreatedAt()))
                .toList();
    }

    @Override
    public List<PlatformAnalyticsRow> findPlatformAnalyticsRows(Instant from, Instant to) {
        return jpa.findPlatformAnalyticsRows(from, to).stream()
                .map(row -> new PlatformAnalyticsRow(
                        row.getMerchantId(),
                        row.getStatus(),
                        row.getTotalAmount(),
                        row.getCurrency(),
                        row.getCreatedAt()))
                .toList();
    }

    @Override
    public List<TopProductRow> findMerchantTopProductRows(String merchantId, Instant from, Instant to) {
        return jpa.findMerchantTopProductRows(merchantId, from, to).stream()
                .map(row -> new TopProductRow(
                        row.getSkuId(),
                        row.getUnitsSold() == null ? 0L : row.getUnitsSold(),
                        row.getRevenue() == null ? java.math.BigDecimal.ZERO : row.getRevenue()))
                .toList();
    }

    @Override
    public List<String> findPendingOrderIdsOlderThan(Instant cutoff, int limit) {
        return jpa.findPendingOrderIdsOlderThan(cutoff, PageRequest.of(0, Math.max(1, limit)));
    }
}
