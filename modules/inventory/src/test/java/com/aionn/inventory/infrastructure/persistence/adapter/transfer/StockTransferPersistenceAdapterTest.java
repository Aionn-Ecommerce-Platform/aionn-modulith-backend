package com.aionn.inventory.infrastructure.persistence.adapter.transfer;

import com.aionn.inventory.domain.model.StockTransfer;
import com.aionn.inventory.infrastructure.persistence.entity.StockTransferEntity;
import com.aionn.inventory.infrastructure.persistence.mapper.StockTransferDomainMapper;
import com.aionn.inventory.infrastructure.persistence.repository.StockTransferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockTransferPersistenceAdapterTest {

    private static final String TRANSFER_ID = "TRA_1";
    private static final String MERCHANT_ID = "M_1";
    private static final String SOURCE_WH = "WH_1";
    private static final String TARGET_WH = "WH_2";
    private static final String SKU_ID = "SKU_1";
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private StockTransferRepository jpa;
    @Mock
    private StockTransferDomainMapper mapper;

    @InjectMocks
    private StockTransferPersistenceAdapter adapter;

    @Test
    void saveMapsThroughEntityAndBack() {
        StockTransfer domain = StockTransfer.initiate(
                TRANSFER_ID, MERCHANT_ID, SOURCE_WH, TARGET_WH, SKU_ID, 10, FIXED_CLOCK);
        StockTransferEntity entity = new StockTransferEntity();
        when(jpa.findById(TRANSFER_ID)).thenReturn(Optional.empty());
        when(mapper.toEntity(eq(domain), any())).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        StockTransfer saved = adapter.save(domain);

        assertThat(saved).isSameAs(domain);
    }

    @Test
    void findByIdReturnsMappedDomainWhenPresent() {
        StockTransferEntity entity = new StockTransferEntity();
        StockTransfer domain = StockTransfer.initiate(
                TRANSFER_ID, MERCHANT_ID, SOURCE_WH, TARGET_WH, SKU_ID, 10, FIXED_CLOCK);
        when(jpa.findById(TRANSFER_ID)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findById(TRANSFER_ID)).contains(domain);
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        when(jpa.findById("missing")).thenReturn(Optional.empty());

        assertThat(adapter.findById("missing")).isEmpty();
    }
}
