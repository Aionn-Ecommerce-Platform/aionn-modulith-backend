package com.aionn.ordering.infrastructure.persistence.adapter.returns;

import com.aionn.ordering.application.port.out.OrderReturnPersistencePort;
import com.aionn.ordering.domain.model.OrderReturn;
import com.aionn.ordering.domain.valueobject.ReturnStatus;
import com.aionn.ordering.infrastructure.persistence.entity.OrderReturnEntity;
import com.aionn.ordering.infrastructure.persistence.mapper.OrderReturnDomainMapper;
import com.aionn.ordering.infrastructure.persistence.repository.OrderReturnRepository;
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
class OrderReturnPersistenceAdapterTest {

    @Mock
    private OrderReturnRepository jpa;

    @Mock
    private OrderReturnDomainMapper mapper;

    @InjectMocks
    private OrderReturnPersistenceAdapter adapter;

    @Test
    void savesNewReturnSuccessfully() {
        OrderReturn orderReturn = mock(OrderReturn.class);
        OrderReturnEntity entity = mock(OrderReturnEntity.class);
        OrderReturnEntity savedEntity = mock(OrderReturnEntity.class);
        OrderReturn savedReturn = mock(OrderReturn.class);

        when(orderReturn.getReturnId()).thenReturn("return-1");
        when(jpa.findById("return-1")).thenReturn(Optional.empty());
        when(mapper.toEntity(orderReturn, null)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(savedReturn);

        OrderReturn result = adapter.save(orderReturn);

        assertThat(result).isEqualTo(savedReturn);
        verify(jpa).findById("return-1");
        verify(mapper).toEntity(orderReturn, null);
        verify(jpa).save(entity);
    }

    @Test
    void updatesExistingReturnSuccessfully() {
        OrderReturn orderReturn = mock(OrderReturn.class);
        OrderReturnEntity existingEntity = mock(OrderReturnEntity.class);
        OrderReturnEntity entity = mock(OrderReturnEntity.class);
        OrderReturnEntity savedEntity = mock(OrderReturnEntity.class);
        OrderReturn savedReturn = mock(OrderReturn.class);

        when(orderReturn.getReturnId()).thenReturn("return-1");
        when(jpa.findById("return-1")).thenReturn(Optional.of(existingEntity));
        when(mapper.toEntity(orderReturn, existingEntity)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(savedReturn);

        OrderReturn result = adapter.save(orderReturn);

        assertThat(result).isEqualTo(savedReturn);
        verify(mapper).toEntity(orderReturn, existingEntity);
    }

    @Test
    void findsReturnByIdWhenExists() {
        String returnId = "return-1";
        OrderReturnEntity entity = mock(OrderReturnEntity.class);
        OrderReturn orderReturn = mock(OrderReturn.class);

        when(jpa.findById(returnId)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(orderReturn);

        Optional<OrderReturn> result = adapter.findById(returnId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(orderReturn);
    }

    @Test
    void returnsEmptyWhenReturnNotFound() {
        String returnId = "non-existent";

        when(jpa.findById(returnId)).thenReturn(Optional.empty());

        Optional<OrderReturn> result = adapter.findById(returnId);

        assertThat(result).isEmpty();
        verify(mapper, never()).toDomain(any());
    }

    @Test
    void findsReturnsByStatus() {
        ReturnStatus status = ReturnStatus.REQUESTED;
        int limit = 10;
        OrderReturnEntity entity1 = mock(OrderReturnEntity.class);
        OrderReturnEntity entity2 = mock(OrderReturnEntity.class);
        OrderReturn return1 = mock(OrderReturn.class);
        OrderReturn return2 = mock(OrderReturn.class);

        when(jpa.findByStatusOrderByRequestedAtDesc(eq("REQUESTED"), any(Pageable.class)))
                .thenReturn(List.of(entity1, entity2));
        when(mapper.toDomain(entity1)).thenReturn(return1);
        when(mapper.toDomain(entity2)).thenReturn(return2);

        List<OrderReturn> result = adapter.findByStatus(status, limit);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(return1, return2);
    }

    @Test
    void enforcesMinimumLimitOfOneWhenFindingByStatus() {
        ReturnStatus status = ReturnStatus.APPROVED;

        when(jpa.findByStatusOrderByRequestedAtDesc(eq("APPROVED"), any(Pageable.class)))
                .thenReturn(List.of());

        adapter.findByStatus(status, 0);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpa).findByStatusOrderByRequestedAtDesc(eq("APPROVED"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    void enforcesMaximumLimitOf200WhenFindingByStatus() {
        ReturnStatus status = ReturnStatus.ITEM_RECEIVED;

        when(jpa.findByStatusOrderByRequestedAtDesc(eq("ITEM_RECEIVED"), any(Pageable.class)))
                .thenReturn(List.of());

        adapter.findByStatus(status, 500);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpa).findByStatusOrderByRequestedAtDesc(eq("ITEM_RECEIVED"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(200);
    }

    @Test
    void findsReturnsByUserId() {
        String userId = "user-1";
        int limit = 20;
        OrderReturnEntity entity = mock(OrderReturnEntity.class);
        OrderReturn orderReturn = mock(OrderReturn.class);

        when(jpa.findByUserIdOrderByRequestedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(orderReturn);

        List<OrderReturn> result = adapter.findByUserId(userId, limit);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(orderReturn);
    }

    @Test
    void enforcesLimitBoundsWhenFindingByUserId() {
        String userId = "user-1";

        when(jpa.findByUserIdOrderByRequestedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(List.of());

        adapter.findByUserId(userId, 300);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpa).findByUserIdOrderByRequestedAtDesc(eq(userId), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(200);
    }

    @Test
    void findsReturnsByMerchantId() {
        String merchantId = "merchant-1";
        int limit = 15;
        OrderReturnEntity entity = mock(OrderReturnEntity.class);
        OrderReturn orderReturn = mock(OrderReturn.class);

        when(jpa.findByMerchantIdOrderByRequestedAtDesc(eq(merchantId), any(Pageable.class)))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(orderReturn);

        List<OrderReturn> result = adapter.findByMerchantId(merchantId, limit);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(orderReturn);
    }

    @Test
    void enforcesLimitBoundsWhenFindingByMerchantId() {
        String merchantId = "merchant-1";

        when(jpa.findByMerchantIdOrderByRequestedAtDesc(eq(merchantId), any(Pageable.class)))
                .thenReturn(List.of());

        adapter.findByMerchantId(merchantId, -5);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpa).findByMerchantIdOrderByRequestedAtDesc(eq(merchantId), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    void findsReturnAnalyticsRows() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-31T23:59:59Z");

        OrderReturnRepository.ReturnAnalyticsProjection projection =
                mock(OrderReturnRepository.ReturnAnalyticsProjection.class);
        when(projection.getStatus()).thenReturn("APPROVED");
        when(projection.getReason()).thenReturn("DEFECTIVE");
        when(projection.getRefundAmount()).thenReturn(new BigDecimal("100.00"));
        when(projection.getRefundCurrency()).thenReturn("VND");

        when(jpa.findReturnAnalyticsRows(from, to))
                .thenReturn(List.of(projection));

        List<OrderReturnPersistencePort.ReturnAnalyticsRow> result =
                adapter.findReturnAnalyticsRows(from, to);

        assertThat(result).hasSize(1);
        OrderReturnPersistencePort.ReturnAnalyticsRow row = result.get(0);
        assertThat(row.status()).isEqualTo("APPROVED");
        assertThat(row.reason()).isEqualTo("DEFECTIVE");
        assertThat(row.refundAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(row.refundCurrency()).isEqualTo("VND");
    }

    @Test
    void countsCompletedOrdersBetween() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-31T23:59:59Z");

        when(jpa.countCompletedOrdersBetween(from, to)).thenReturn(42L);

        long result = adapter.countCompletedOrdersBetween(from, to);

        assertThat(result).isEqualTo(42L);
        verify(jpa).countCompletedOrdersBetween(from, to);
    }
}
