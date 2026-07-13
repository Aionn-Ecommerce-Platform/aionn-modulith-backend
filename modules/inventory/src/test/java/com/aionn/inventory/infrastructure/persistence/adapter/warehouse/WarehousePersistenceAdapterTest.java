package com.aionn.inventory.infrastructure.persistence.adapter.warehouse;

import com.aionn.inventory.domain.model.Warehouse;
import com.aionn.inventory.infrastructure.persistence.entity.WarehouseEntity;
import com.aionn.inventory.infrastructure.persistence.mapper.WarehouseDomainMapper;
import com.aionn.inventory.infrastructure.persistence.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehousePersistenceAdapterTest {

    private static final String WAREHOUSE_ID = "WH_1";
    private static final String MERCHANT_ID = "M_1";
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private WarehouseRepository jpa;
    @Mock
    private WarehouseDomainMapper mapper;

    @InjectMocks
    private WarehousePersistenceAdapter adapter;

    @Test
    void saveMapsThroughEntityAndBack() {
        Warehouse domain = Warehouse.create(WAREHOUSE_ID, MERCHANT_ID, "addr", 1, FIXED_CLOCK);
        WarehouseEntity entity = new WarehouseEntity();
        when(jpa.findById(WAREHOUSE_ID)).thenReturn(Optional.empty());
        when(mapper.toEntity(eq(domain), any())).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        Warehouse saved = adapter.save(domain);

        assertThat(saved).isSameAs(domain);
    }

    @Test
    void findByIdReturnsMappedDomainWhenPresent() {
        WarehouseEntity entity = new WarehouseEntity();
        Warehouse domain = Warehouse.create(WAREHOUSE_ID, MERCHANT_ID, "addr", 1, FIXED_CLOCK);
        when(jpa.findById(WAREHOUSE_ID)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findById(WAREHOUSE_ID)).contains(domain);
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        when(jpa.findById("missing")).thenReturn(Optional.empty());

        assertThat(adapter.findById("missing")).isEmpty();
    }

    @Test
    void findByMerchantOrderByPriorityMapsResults() {
        WarehouseEntity entity = new WarehouseEntity();
        Warehouse domain = Warehouse.create(WAREHOUSE_ID, MERCHANT_ID, "addr", 1, FIXED_CLOCK);
        when(jpa.findByMerchantIdOrderByPriorityLevelAsc(MERCHANT_ID)).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findByMerchantOrderByPriority(MERCHANT_ID)).containsExactly(domain);
    }
}
