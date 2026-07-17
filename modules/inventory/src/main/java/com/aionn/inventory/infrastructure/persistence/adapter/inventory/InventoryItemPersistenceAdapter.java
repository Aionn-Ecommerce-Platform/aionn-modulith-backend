package com.aionn.inventory.infrastructure.persistence.adapter.inventory;

import com.aionn.inventory.application.port.out.InventoryItemPersistencePort;
import com.aionn.inventory.domain.model.InventoryItem;
import com.aionn.inventory.domain.valueobject.InventoryItemKey;
import com.aionn.inventory.infrastructure.persistence.entity.InventoryItemEntity;
import com.aionn.inventory.infrastructure.persistence.mapper.InventoryItemDomainMapper;
import com.aionn.inventory.infrastructure.persistence.repository.InventoryItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InventoryItemPersistenceAdapter implements InventoryItemPersistencePort {

    private final InventoryItemRepository jpa;
    private final InventoryItemDomainMapper mapper;

    @Override
    public InventoryItem save(InventoryItem item) {
        InventoryItemEntity.InventoryItemId id = new InventoryItemEntity.InventoryItemId(
                item.getKey().skuId(), item.getKey().warehouseId());
        InventoryItemEntity existing = jpa.findById(id).orElse(null);
        InventoryItemEntity entity = mapper.toEntity(item, existing);
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<InventoryItem> findByKey(InventoryItemKey key) {
        return jpa.findById(toId(key)).map(mapper::toDomain);
    }

    @Override
    public Optional<InventoryItem> lockByKey(InventoryItemKey key) {
        return jpa.findForUpdate(key.skuId(), key.warehouseId()).map(mapper::toDomain);
    }

    @Override
    public List<InventoryItem> findBySkuAcrossWarehouses(String skuId, List<String> warehouseIds) {
        if (warehouseIds == null || warehouseIds.isEmpty()) {
            return List.of();
        }
        return jpa.findByIdSkuIdAndIdWarehouseIdIn(skuId, warehouseIds).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<InventoryItem> findBySku(String skuId) {
        return jpa.findByIdSkuId(skuId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<InventoryItem> findByWarehouse(String warehouseId, Pageable pageable) {
        return jpa.findByIdWarehouseId(warehouseId, pageable).map(mapper::toDomain);
    }

    private static InventoryItemEntity.InventoryItemId toId(InventoryItemKey key) {
        return new InventoryItemEntity.InventoryItemId(key.skuId(), key.warehouseId());
    }
}

