package com.aionn.recommendation.infrastructure.persistence.adapter;

import com.aionn.recommendation.application.port.out.PopularityPersistencePort;
import com.aionn.recommendation.domain.model.ProductPopularity;
import com.aionn.recommendation.infrastructure.persistence.mapper.RecommendationSignalDomainMapper;
import com.aionn.recommendation.infrastructure.persistence.repository.ProductPopularityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PopularityPersistenceAdapter implements PopularityPersistencePort {

    private final ProductPopularityRepository jpa;
    private final RecommendationSignalDomainMapper mapper;

    @Override
    public List<ProductPopularity> findTop(int limit) {
        return jpa.findTop(Math.max(1, limit)).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ProductPopularity> findByProductIds(Collection<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return jpa.findByProductIdIn(productIds).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void upsertAll(List<ProductPopularity> popularities) {
        if (popularities == null || popularities.isEmpty()) {
            return;
        }
        for (ProductPopularity popularity : popularities) {
            jpa.upsert(
                    popularity.productId(),
                    popularity.score(),
                    popularity.viewCount(),
                    popularity.purchaseCount(),
                    popularity.computedAt());
        }
    }

    @Override
    public int deleteComputedBefore(Instant cutoff) {
        return jpa.deleteComputedBefore(cutoff);
    }
}
