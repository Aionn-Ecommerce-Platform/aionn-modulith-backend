package com.aionn.inventory.infrastructure.persistence.adapter.adjustment;

import com.aionn.inventory.domain.model.StockAdjustment;
import com.aionn.inventory.domain.valueobject.AdjustmentType;
import com.aionn.inventory.infrastructure.persistence.entity.StockAdjustmentEntity;
import com.aionn.inventory.infrastructure.persistence.mapper.StockAdjustmentDomainMapper;
import com.aionn.inventory.infrastructure.persistence.repository.StockAdjustmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockAdjustmentPersistenceAdapterTest {

    private static final String ADJUSTMENT_ID = "ADJ_1";
    private static final String SKU_ID = "SKU_1";
    private static final String WAREHOUSE_ID = "WH_1";
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private StockAdjustmentRepository jpa;
    @Mock
    private StockAdjustmentDomainMapper mapper;

    @InjectMocks
    private StockAdjustmentPersistenceAdapter adapter;

    @Test
    void saveMapsThroughEntityAndBack() {
        StockAdjustment domain = StockAdjustment.manual(
                ADJUSTMENT_ID, SKU_ID, WAREHOUSE_ID, 10,
                AdjustmentType.MANUAL_INCREASE, "restock", FIXED_CLOCK);
        StockAdjustmentEntity entity = new StockAdjustmentEntity();
        when(mapper.toEntity(domain)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        StockAdjustment saved = adapter.save(domain);

        assertThat(saved).isSameAs(domain);
    }
}
