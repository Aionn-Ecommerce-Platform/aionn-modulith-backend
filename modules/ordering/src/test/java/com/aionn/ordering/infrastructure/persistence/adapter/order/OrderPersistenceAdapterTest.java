package com.aionn.ordering.infrastructure.persistence.adapter.order;

import com.aionn.ordering.application.port.out.OrderPersistencePort;
import com.aionn.ordering.domain.model.Order;
import com.aionn.ordering.infrastructure.persistence.entity.OrderEntity;
import com.aionn.ordering.infrastructure.persistence.mapper.OrderDomainMapper;
import com.aionn.ordering.infrastructure.persistence.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPersistenceAdapterTest {

    @Mock
    private OrderRepository jpa;

    @Mock
    private OrderDomainMapper mapper;

    @InjectMocks
    private OrderPersistenceAdapter adapter;

    @Test
    void savesNewOrderSuccessfully() {
        Order order = mock(Order.class);
        OrderEntity entity = mock(OrderEntity.class);
        OrderEntity savedEntity = mock(OrderEntity.class);
        Order savedOrder = mock(Order.class);

        when(order.getOrderId()).thenReturn("order-1");
        when(jpa.findById("order-1")).thenReturn(Optional.empty());
        when(mapper.toEntity(order, null)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(savedOrder);

        Order result = adapter.save(order);

        assertThat(result).isEqualTo(savedOrder);
        verify(jpa).findById("order-1");
        verify(mapper).toEntity(order, null);
        verify(jpa).save(entity);
    }

    @Test
    void updatesExistingOrderSuccessfully() {
        Order order = mock(Order.class);
        OrderEntity existingEntity = mock(OrderEntity.class);
        OrderEntity entity = mock(OrderEntity.class);
        OrderEntity savedEntity = mock(OrderEntity.class);
        Order savedOrder = mock(Order.class);

        when(order.getOrderId()).thenReturn("order-1");
        when(jpa.findById("order-1")).thenReturn(Optional.of(existingEntity));
        when(mapper.toEntity(order, existingEntity)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(savedOrder);

        Order result = adapter.save(order);

        assertThat(result).isEqualTo(savedOrder);
        verify(mapper).toEntity(order, existingEntity);
    }

    @Test
    void findsOrderByIdWhenExists() {
        String orderId = "order-1";
        OrderEntity entity = mock(OrderEntity.class);
        Order order = mock(Order.class);

        when(jpa.findById(orderId)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(order);

        Optional<Order> result = adapter.findById(orderId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(order);
    }

    @Test
    void returnsEmptyWhenOrderNotFound() {
        String orderId = "non-existent";

        when(jpa.findById(orderId)).thenReturn(Optional.empty());

        Optional<Order> result = adapter.findById(orderId);

        assertThat(result).isEmpty();
        verify(mapper, never()).toDomain(any());
    }

    @Test
    void findsOrdersByUserWithLimit() {
        String userId = "user-1";
        int limit = 10;
        OrderEntity entity1 = mock(OrderEntity.class);
        OrderEntity entity2 = mock(OrderEntity.class);
        Order order1 = mock(Order.class);
        Order order2 = mock(Order.class);

        when(jpa.findByUserIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(List.of(entity1, entity2));
        when(mapper.toDomain(entity1)).thenReturn(order1);
        when(mapper.toDomain(entity2)).thenReturn(order2);

        List<Order> result = adapter.findByUser(userId, limit);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(order1, order2);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpa).findByUserIdOrderByCreatedAtDesc(eq(userId), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void enforcesMinimumLimitOfOneWhenFindingByUser() {
        String userId = "user-1";

        when(jpa.findByUserIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(List.of());

        adapter.findByUser(userId, 0);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpa).findByUserIdOrderByCreatedAtDesc(eq(userId), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    void findsOrdersByUserAndStatuses() {
        String userId = "user-1";
        List<String> statuses = List.of("PENDING", "APPROVED");
        int limit = 5;
        OrderEntity entity = mock(OrderEntity.class);
        Order order = mock(Order.class);

        when(jpa.findByUserIdAndStatusInOrderByCreatedAtDesc(eq(userId), eq(statuses), any(Pageable.class)))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(order);

        List<Order> result = adapter.findByUserAndStatuses(userId, statuses, limit);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(order);
    }

    @Test
    void findsOrdersByMerchant() {
        String merchantId = "merchant-1";
        int limit = 10;
        OrderEntity entity = mock(OrderEntity.class);
        Order order = mock(Order.class);

        when(jpa.findByMerchantIdOrderByCreatedAtDesc(eq(merchantId), any(Pageable.class)))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(order);

        List<Order> result = adapter.findByMerchant(merchantId, limit);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(order);
    }

    @Test
    void findsOrdersByMerchantAndStatuses() {
        String merchantId = "merchant-1";
        List<String> statuses = List.of("COMPLETED");
        int limit = 20;
        OrderEntity entity = mock(OrderEntity.class);
        Order order = mock(Order.class);

        when(jpa.findByMerchantIdAndStatusInOrderByCreatedAtDesc(
                eq(merchantId), eq(statuses), any(Pageable.class)))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(order);

        List<Order> result = adapter.findByMerchantAndStatuses(merchantId, statuses, limit);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(order);
    }

    @Test
    void findsMerchantAnalyticsRows() {
        String merchantId = "merchant-1";
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-31T23:59:59Z");

        OrderRepository.OrderAnalyticsProjection projection = mock(OrderRepository.OrderAnalyticsProjection.class);
        when(projection.getStatus()).thenReturn("COMPLETED");
        when(projection.getTotalAmount()).thenReturn(new BigDecimal("1000.00"));
        when(projection.getCurrency()).thenReturn("VND");
        when(projection.getCreatedAt()).thenReturn(from);

        when(jpa.findMerchantAnalyticsRows(merchantId, from, to))
                .thenReturn(List.of(projection));

        List<OrderPersistencePort.OrderAnalyticsRow> result =
                adapter.findMerchantAnalyticsRows(merchantId, from, to);

        assertThat(result).hasSize(1);
        OrderPersistencePort.OrderAnalyticsRow row = result.get(0);
        assertThat(row.status()).isEqualTo("COMPLETED");
        assertThat(row.totalAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(row.currency()).isEqualTo("VND");
        assertThat(row.createdAt()).isEqualTo(from);
    }

    @Test
    void findsPlatformAnalyticsRows() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-31T23:59:59Z");

        OrderRepository.PlatformAnalyticsProjection projection =
                mock(OrderRepository.PlatformAnalyticsProjection.class);
        when(projection.getMerchantId()).thenReturn("merchant-1");
        when(projection.getStatus()).thenReturn("COMPLETED");
        when(projection.getTotalAmount()).thenReturn(new BigDecimal("500.00"));
        when(projection.getCurrency()).thenReturn("VND");
        when(projection.getCreatedAt()).thenReturn(from);

        when(jpa.findPlatformAnalyticsRows(from, to))
                .thenReturn(List.of(projection));

        List<OrderPersistencePort.PlatformAnalyticsRow> result =
                adapter.findPlatformAnalyticsRows(from, to);

        assertThat(result).hasSize(1);
        OrderPersistencePort.PlatformAnalyticsRow row = result.get(0);
        assertThat(row.merchantId()).isEqualTo("merchant-1");
        assertThat(row.status()).isEqualTo("COMPLETED");
        assertThat(row.totalAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void findsMerchantTopProductRows() {
        String merchantId = "merchant-1";
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-31T23:59:59Z");

        OrderRepository.TopProductProjection projection = mock(OrderRepository.TopProductProjection.class);
        when(projection.getSkuId()).thenReturn("sku-1");
        when(projection.getUnitsSold()).thenReturn(100L);
        when(projection.getRevenue()).thenReturn(new BigDecimal("50000"));

        when(jpa.findMerchantTopProductRows(merchantId, from, to))
                .thenReturn(List.of(projection));

        List<OrderPersistencePort.TopProductRow> result =
                adapter.findMerchantTopProductRows(merchantId, from, to);

        assertThat(result).hasSize(1);
        OrderPersistencePort.TopProductRow row = result.get(0);
        assertThat(row.skuId()).isEqualTo("sku-1");
        assertThat(row.unitsSold()).isEqualTo(100L);
        assertThat(row.revenue()).isEqualByComparingTo(new BigDecimal("50000"));
    }

    @Test
    void handlesNullValuesInTopProductProjection() {
        String merchantId = "merchant-1";
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-31T23:59:59Z");

        OrderRepository.TopProductProjection projection = mock(OrderRepository.TopProductProjection.class);
        when(projection.getSkuId()).thenReturn("sku-1");
        when(projection.getUnitsSold()).thenReturn(null);
        when(projection.getRevenue()).thenReturn(null);

        when(jpa.findMerchantTopProductRows(merchantId, from, to))
                .thenReturn(List.of(projection));

        List<OrderPersistencePort.TopProductRow> result =
                adapter.findMerchantTopProductRows(merchantId, from, to);

        assertThat(result).hasSize(1);
        OrderPersistencePort.TopProductRow row = result.get(0);
        assertThat(row.unitsSold()).isEqualTo(0L);
        assertThat(row.revenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void findsPendingOrderIdsOlderThanCutoff() {
        Instant cutoff = Instant.parse("2026-01-01T00:00:00Z");
        int limit = 50;

        when(jpa.findPendingOrderIdsOlderThan(eq(cutoff), any(Pageable.class)))
                .thenReturn(List.of("order-1", "order-2", "order-3"));

        List<String> result = adapter.findPendingOrderIdsOlderThan(cutoff, limit);

        assertThat(result).hasSize(3);
        assertThat(result).containsExactly("order-1", "order-2", "order-3");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpa).findPendingOrderIdsOlderThan(eq(cutoff), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }
}
