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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        InventoryItem domain = InventoryItem.initialize(KEY, 100);
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
        InventoryItem domain = InventoryItem.initialize(KEY, 100);
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
        InventoryItem domain = InventoryItem.initialize(KEY, 100);
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
        InventoryItem domain = InventoryItem.initialize(KEY, 100);
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
    void findBySkuAcrossWarehousesReturnsEmptyForEmptyWarehouseIds() {
        assertThat(adapter.findBySkuAcrossWarehouses(SKU_ID, List.of())).isEmpty();
        assertThat(adapter.findBySkuAcrossWarehouses(SKU_ID, null)).isEmpty();
    }

    @Test
    void findBySkuAcrossWarehousesMapsResults() {
        InventoryItemEntity entity = new InventoryItemEntity();
        InventoryItem domain = InventoryItem.initialize(KEY, 100);
        when(jpa.findByIdSkuIdAndIdWarehouseIdIn(SKU_ID, List.of(WAREHOUSE_ID)))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findBySkuAcrossWarehouses(SKU_ID, List.of(WAREHOUSE_ID)))
                .containsExactly(domain);
    }

    @Test
    void findBySkuMapsResults() {
        InventoryItemEntity entity = new InventoryItemEntity();
        InventoryItem domain = InventoryItem.initialize(KEY, 100);
        when(jpa.findByIdSkuId(SKU_ID)).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findBySku(SKU_ID)).containsExactly(domain);
    }

    @Test
    void findByWarehouseMapsResults() {
        InventoryItemEntity entity = new InventoryItemEntity();
        InventoryItem domain = InventoryItem.initialize(KEY, 100);
        when(jpa.findByIdWarehouseId(eq(WAREHOUSE_ID), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findByWarehouse(WAREHOUSE_ID, PageRequest.of(0, 10)))
                .containsExactly(domain);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(jpa).findByIdWarehouseId(eq(WAREHOUSE_ID), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
    }
}
