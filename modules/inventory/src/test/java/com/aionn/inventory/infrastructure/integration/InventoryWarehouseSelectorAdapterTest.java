package com.aionn.inventory.infrastructure.integration;

import com.aionn.inventory.application.port.out.InventoryItemPersistencePort;
import com.aionn.inventory.application.port.out.WarehousePersistencePort;
import com.aionn.inventory.domain.model.InventoryItem;
import com.aionn.inventory.domain.model.Warehouse;
import com.aionn.inventory.domain.valueobject.InventoryItemKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryWarehouseSelectorAdapterTest {

    @Mock
    private WarehousePersistencePort warehouseRepository;
    @Mock
    private InventoryItemPersistencePort inventoryItemRepository;

    private InventoryWarehouseSelectorAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new InventoryWarehouseSelectorAdapter(warehouseRepository, inventoryItemRepository);
    }

    @Test
    void selectWarehouseReturnsEmptyOnBlankInputs() {
        assertThat(adapter.selectWarehouseForSku(null, "SKU_1")).isEmpty();
        assertThat(adapter.selectWarehouseForSku("M_1", "")).isEmpty();
    }

    @Test
    void selectWarehouseReturnsEmptyWhenNoWarehousesFound() {
        when(warehouseRepository.findByMerchantOrderByPriority("M_1")).thenReturn(List.of());
        assertThat(adapter.selectWarehouseForSku("M_1", "SKU_1")).isEmpty();
    }

    @Test
    void selectWarehouseReturnsHighestPriorityWithAvailableStock() {
        Warehouse w1 = Warehouse.create("WH_1", "M_1", "addr-1", 1);
        Warehouse w2 = Warehouse.create("WH_2", "M_1", "addr-2", 2);
        when(warehouseRepository.findByMerchantOrderByPriority("M_1")).thenReturn(List.of(w1, w2));

        InventoryItem item1 = InventoryItem.initialize(new InventoryItemKey("SKU_1", "WH_1"), 0);
        InventoryItem item2 = InventoryItem.initialize(new InventoryItemKey("SKU_1", "WH_2"), 5);
        when(inventoryItemRepository.findBySkuAcrossWarehouses("SKU_1", List.of("WH_1", "WH_2")))
                .thenReturn(List.of(item1, item2));

        Optional<String> result = adapter.selectWarehouseForSku("M_1", "SKU_1");

        assertThat(result).hasValue("WH_2");
    }

    @Test
    void selectWarehouseFallsBackToFirstWarehouseWhenNoStockAvailable() {
        Warehouse w1 = Warehouse.create("WH_1", "M_1", "addr-1", 1);
        Warehouse w2 = Warehouse.create("WH_2", "M_1", "addr-2", 2);
        when(warehouseRepository.findByMerchantOrderByPriority("M_1")).thenReturn(List.of(w1, w2));

        InventoryItem item1 = InventoryItem.initialize(new InventoryItemKey("SKU_1", "WH_1"), 0);
        InventoryItem item2 = InventoryItem.initialize(new InventoryItemKey("SKU_1", "WH_2"), 0);
        when(inventoryItemRepository.findBySkuAcrossWarehouses("SKU_1", List.of("WH_1", "WH_2")))
                .thenReturn(List.of(item1, item2));

        Optional<String> result = adapter.selectWarehouseForSku("M_1", "SKU_1");

        assertThat(result).hasValue("WH_1");
    }
}
