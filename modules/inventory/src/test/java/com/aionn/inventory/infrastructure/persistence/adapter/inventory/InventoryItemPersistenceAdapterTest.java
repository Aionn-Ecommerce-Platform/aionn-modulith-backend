package com.aionn.inventory.infrastructure.persistence.adapter.inventory;

import com.aionn.inventory.domain.model.InventoryItem;
import com.aionn.inventory.domain.valueobject.InventoryItemKey;
import com.aionn.inventory.infrastructure.persistence.entity.InventoryItemEntity;
import com.aionn.inventory.infrastructure.persistence.mapper.InventoryItemDomainMapper;
import com.aionn.inventory.infrastructure.persistence.repository.InventoryItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryItemPersistenceAdapterTest {

    private static final String SKU_ID = "SKU_1";
    private static final String WAREHOUSE_ID = "WH_1";
    private static final InventoryItemKey KEY = new InventoryItemKey(SKU_ID, WAREHOUSE_ID);

    @Mock
    private InventoryItemRepository jpa;
    @Mock
    private InventoryItemDomainMapper mapper;

    @InjectMocks
    private InventoryItemPersistenceAdapter adapter;

    @Test
    void saveMapsThroughEntityAndBack() {
        InventoryItem domain = InventoryItem.initialize(KEY, 100, java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        InventoryItemEntity entity = new InventoryItemEntity();
        InventoryItemEntity.InventoryItemId id = new InventoryItemEntity.InventoryItemId(SKU_ID, WAREHOUSE_ID);
        when(jpa.findById(id)).thenReturn(Optional.empty());
        when(mapper.toEntity(domain, null)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        InventoryItem saved = adapter.save(domain);

        assertThat(saved).isSameAs(domain);
        verify(mapper).toEntity(domain, null);
    }

    @Test
    void saveUsesExistingEntityWhenPresent() {
        InventoryItem domain = InventoryItem.initialize(KEY, 100, java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        InventoryItemEntity existing = new InventoryItemEntity();
        InventoryItemEntity updated = new InventoryItemEntity();
        InventoryItemEntity.InventoryItemId id = new InventoryItemEntity.InventoryItemId(SKU_ID, WAREHOUSE_ID);
        when(jpa.findById(id)).thenReturn(Optional.of(existing));
        when(mapper.toEntity(domain, existing)).thenReturn(updated);
        when(jpa.save(updated)).thenReturn(updated);
        when(mapper.toDomain(updated)).thenReturn(domain);

        adapter.save(domain);

        verify(mapper).toEntity(domain, existing);
    }

    @Test
    void findByKeyReturnsMappedDomainWhenPresent() {
        InventoryItemEntity entity = new InventoryItemEntity();
        InventoryItem domain = InventoryItem.initialize(KEY, 100, java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        InventoryItemEntity.InventoryItemId id = new InventoryItemEntity.InventoryItemId(SKU_ID, WAREHOUSE_ID);
        when(jpa.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findByKey(KEY)).contains(domain);
    }

    @Test
    void findByKeyReturnsEmptyWhenMissing() {
        InventoryItemEntity.InventoryItemId id = new InventoryItemEntity.InventoryItemId(SKU_ID, WAREHOUSE_ID);
        when(jpa.findById(id)).thenReturn(Optional.empty());

        assertThat(adapter.findByKey(KEY)).isEmpty();
    }

    @Test
    void lockByKeyReturnsMappedDomain() {
        InventoryItemEntity entity = new InventoryItemEntity();
        InventoryItem domain = InventoryItem.initialize(KEY, 100, java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        when(jpa.findForUpdate(SKU_ID, WAREHOUSE_ID)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.lockByKey(KEY)).contains(domain);
    }

    @Test
    void lockByKeyReturnsEmptyWhenMissing() {
        when(jpa.findForUpdate(SKU_ID, WAREHOUSE_ID)).thenReturn(Optional.empty());

        assertThat(adapter.lockByKey(KEY)).isEmpty();
    }

    @Test
    void createIfAbsentThenLocksTheWinningItem() {
        Instant now = Instant.parse("2026-08-05T00:00:00Z");
        InventoryItemEntity entity = new InventoryItemEntity();
        InventoryItem domain = InventoryItem.initialize(KEY, 0, java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        when(jpa.findForUpdate(SKU_ID, WAREHOUSE_ID)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.createIfAbsentAndLock(KEY, now)).isSameAs(domain);
        verify(jpa).insertIfAbsent(SKU_ID, WAREHOUSE_ID, now);
        verify(jpa).findForUpdate(SKU_ID, WAREHOUSE_ID);
    }

    @Test
    void findBySkuAcrossWarehousesReturnsEmptyForEmptyWarehouseIds() {
        assertThat(adapter.findBySkuAcrossWarehouses(SKU_ID, List.of())).isEmpty();
        assertThat(adapter.findBySkuAcrossWarehouses(SKU_ID, null)).isEmpty();
    }

    @Test
    void findBySkuAcrossWarehousesMapsResults() {
        InventoryItemEntity entity = new InventoryItemEntity();
        InventoryItem domain = InventoryItem.initialize(KEY, 100, java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        when(jpa.findByIdSkuIdAndIdWarehouseIdIn(SKU_ID, List.of(WAREHOUSE_ID)))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findBySkuAcrossWarehouses(SKU_ID, List.of(WAREHOUSE_ID)))
                .containsExactly(domain);
    }

    @Test
    void findBySkuMapsResults() {
        InventoryItemEntity entity = new InventoryItemEntity();
        InventoryItem domain = InventoryItem.initialize(KEY, 100, java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        when(jpa.findByIdSkuId(SKU_ID)).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findBySku(SKU_ID)).containsExactly(domain);
    }

    @Test
    void findByWarehouseMapsResults() {
        InventoryItemEntity entity = new InventoryItemEntity();
        InventoryItem domain = InventoryItem.initialize(KEY, 100, java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        when(jpa.findByIdWarehouseIdOrderByIdSkuIdAsc(eq(WAREHOUSE_ID), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findByWarehouse(WAREHOUSE_ID, OffsetPagination.of(0, 10)).content())
                .containsExactly(domain);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(jpa).findByIdWarehouseIdOrderByIdSkuIdAsc(eq(WAREHOUSE_ID), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        assertThat(captor.getValue().getSort().isUnsorted()).isTrue();
    }

    @Test
    void availabilityIsAskedForInOneQueryRatherThanPerSku() {
        // This runs on the recommendation read path outside the cache, over every SKU of an overfetched
        // slate, so a per-SKU lookup turns one request into a few hundred round trips.
        when(jpa.findAvailableSkuIds(any())).thenReturn(List.of("SKU_1", "SKU_2"));

        assertThat(adapter.findAvailableSkus(List.of("SKU_1", "SKU_2", "SKU_3")))
                .containsExactly("SKU_1", "SKU_2");

        verify(jpa, times(1)).findAvailableSkuIds(any());
    }

    @Test
    void anEmptyOrAbsentSkuSetNeverReachesTheDatabase() {
        assertThat(adapter.findAvailableSkus(List.of())).isEmpty();
        assertThat(adapter.findAvailableSkus(null)).isEmpty();

        verify(jpa, never()).findAvailableSkuIds(any());
    }

    @Test
    void blankSkuIdsAreDroppedBeforeBinding() {
        // A slate can carry a null SKU where a product has no variant; binding it would match nothing and
        // only spend a parameter.
        when(jpa.findAvailableSkuIds(any())).thenReturn(List.of("SKU_1"));

        java.util.List<String> requested = new java.util.ArrayList<>();
        requested.add("SKU_1");
        requested.add(null);
        requested.add("   ");

        assertThat(adapter.findAvailableSkus(requested)).containsExactly("SKU_1");

        ArgumentCaptor<java.util.Collection<String>> captor = ArgumentCaptor.captor();
        verify(jpa).findAvailableSkuIds(captor.capture());
        assertThat(captor.getValue()).containsExactly("SKU_1");
    }

    @Test
    void aSkuRequestedTwiceIsBoundOnce() {
        // Several products in one slate can share a SKU, and repeats only waste the parameter budget.
        when(jpa.findAvailableSkuIds(any())).thenReturn(List.of("SKU_1"));

        adapter.findAvailableSkus(List.of("SKU_1", "SKU_1", "SKU_1"));

        ArgumentCaptor<java.util.Collection<String>> captor = ArgumentCaptor.captor();
        verify(jpa).findAvailableSkuIds(captor.capture());
        assertThat(captor.getValue()).containsExactly("SKU_1");
    }

    @Test
    void aSkuSetLargerThanTheParameterBudgetIsSplitAcrossQueries() {
        // Postgres caps bind parameters at 65535; a large page must not be allowed to approach it.
        List<String> skuIds = new java.util.ArrayList<>();
        for (int index = 0; index < 2500; index++) {
            skuIds.add("SKU_" + index);
        }
        when(jpa.findAvailableSkuIds(any())).thenReturn(List.of());

        adapter.findAvailableSkus(skuIds);

        ArgumentCaptor<java.util.Collection<String>> captor = ArgumentCaptor.captor();
        verify(jpa, times(3)).findAvailableSkuIds(captor.capture());
        assertThat(captor.getAllValues()).extracting(java.util.Collection::size)
                .containsExactly(1000, 1000, 500);
        assertThat(captor.getAllValues().stream()
                .flatMap(java.util.Collection::stream).distinct().count()).isEqualTo(2500);
    }

    @Test
    void availabilityFromEveryChunkIsCombined() {
        List<String> skuIds = new java.util.ArrayList<>();
        for (int index = 0; index < 1200; index++) {
            skuIds.add("SKU_" + index);
        }
        when(jpa.findAvailableSkuIds(any()))
                .thenReturn(List.of("SKU_0"))
                .thenReturn(List.of("SKU_1199"));

        assertThat(adapter.findAvailableSkus(skuIds)).containsExactly("SKU_0", "SKU_1199");
    }
}
