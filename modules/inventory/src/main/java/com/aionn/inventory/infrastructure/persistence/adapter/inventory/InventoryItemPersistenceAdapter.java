package com.aionn.inventory.infrastructure.persistence.adapter.inventory;

import com.aionn.inventory.application.port.out.InventoryItemPersistencePort;
import com.aionn.inventory.application.dto.common.PageResult;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import com.aionn.inventory.domain.model.InventoryItem;
import com.aionn.inventory.domain.valueobject.InventoryItemKey;
import com.aionn.inventory.infrastructure.persistence.entity.InventoryItemEntity;
import com.aionn.inventory.infrastructure.persistence.mapper.InventoryItemDomainMapper;
import com.aionn.inventory.infrastructure.persistence.repository.InventoryItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.time.Instant;

@Repository
@RequiredArgsConstructor
public class InventoryItemPersistenceAdapter implements InventoryItemPersistencePort {

    /**
     * SKUs per availability query. Postgres caps bind parameters at 65535; this stays far under it
     * while still resolving a whole recommendation slate in one round trip.
     */
    private static final int AVAILABILITY_QUERY_CHUNK = 1_000;

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
    public InventoryItem createIfAbsentAndLock(InventoryItemKey key, Instant now) {
        jpa.insertIfAbsent(key.skuId(), key.warehouseId(), now);
        return jpa.findForUpdate(key.skuId(), key.warehouseId())
                .map(mapper::toDomain)
                .orElseThrow(() -> new IllegalStateException("Inventory insert completed without a readable row"));
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
    public Set<String> findAvailableSkus(Collection<String> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Set.of();
        }
        // Deduplicate before binding: a slate can carry the same SKU through several products, and
        // repeated bind parameters waste the statement's parameter budget for nothing.
        Set<String> distinct = new LinkedHashSet<>(skuIds);
        distinct.removeIf(id -> id == null || id.isBlank());
        if (distinct.isEmpty()) {
            return Set.of();
        }
        Set<String> available = new LinkedHashSet<>();
        // Chunked so a large page cannot exceed the driver's bind-parameter limit.
        List<String> all = new ArrayList<>(distinct);
        for (int from = 0; from < all.size(); from += AVAILABILITY_QUERY_CHUNK) {
            List<String> chunk = all.subList(from, Math.min(from + AVAILABILITY_QUERY_CHUNK, all.size()));
            available.addAll(jpa.findAvailableSkuIds(chunk));
        }
        return available;
    }

    @Override
    public PageResult<InventoryItem> findByWarehouse(String warehouseId, OffsetPagination pagination) {
        var page = jpa.findByIdWarehouseIdOrderByIdSkuIdAsc(
                warehouseId, PageRequest.of(pagination.page(), pagination.size())).map(mapper::toDomain);
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getNumberOfElements(), page.getTotalElements());
    }

    @Override
    public List<LowStockItem> findLowStockForMerchant(String merchantId) {
        return jpa.findLowStockForMerchant(merchantId).stream()
                .map(row -> new LowStockItem(
                        row.getSkuId(),
                        row.getWarehouseId(),
                        valueOrZero(row.getPhysicalQty()),
                        valueOrZero(row.getAvailableQty()),
                        valueOrZero(row.getSafetyStockQty())))
                .toList();
    }

    @Override
    public PageResult<InventoryItem> findAllLowStock(OffsetPagination pagination) {
        var page = jpa.findAllLowStock(PageRequest.of(pagination.page(), pagination.size())).map(mapper::toDomain);
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getNumberOfElements(), page.getTotalElements());
    }

    private static int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static InventoryItemEntity.InventoryItemId toId(InventoryItemKey key) {
        return new InventoryItemEntity.InventoryItemId(key.skuId(), key.warehouseId());
    }
}

