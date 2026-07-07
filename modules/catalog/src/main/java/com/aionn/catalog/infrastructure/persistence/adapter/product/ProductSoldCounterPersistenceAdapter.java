package com.aionn.catalog.infrastructure.persistence.adapter.product;

import com.aionn.catalog.application.port.out.product.ProductSoldCounterPersistencePort;
import com.aionn.catalog.infrastructure.persistence.entity.ProductSoldCounterEntity;
import com.aionn.catalog.infrastructure.persistence.repository.product.ProductSoldCounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ProductSoldCounterPersistenceAdapter implements ProductSoldCounterPersistencePort {

    private final ProductSoldCounterRepository jpa;

    @Override
    public long getSoldCount(String productId) {
        return jpa.findById(productId).map(ProductSoldCounterEntity::getSoldCount).orElse(0L);
    }

    @Override
    public Map<String, Long> getSoldCountsByProductIds(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        Map<String, Long> result = new HashMap<>();
        for (ProductSoldCounterEntity e : jpa.findAllByProductIdIn(productIds)) {
            result.put(e.getProductId(), e.getSoldCount());
        }
        return result;
    }

    @Override
    public void incrementSoldCount(String productId, long delta) {
        ProductSoldCounterEntity entity = jpa.findById(productId)
                .orElseGet(() -> ProductSoldCounterEntity.builder()
                        .productId(productId)
                        .soldCount(0L)
                        .build());
        entity.setSoldCount(entity.getSoldCount() + delta);
        jpa.save(entity);
    }
}
