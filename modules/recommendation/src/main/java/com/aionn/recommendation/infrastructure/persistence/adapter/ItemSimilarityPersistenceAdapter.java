package com.aionn.recommendation.infrastructure.persistence.adapter;

import com.aionn.recommendation.application.port.out.ItemSimilarityPersistencePort;
import com.aionn.recommendation.domain.model.ItemSimilarity;
import com.aionn.recommendation.infrastructure.persistence.mapper.RecommendationSignalDomainMapper;
import com.aionn.recommendation.infrastructure.persistence.repository.ItemSimilarityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ItemSimilarityPersistenceAdapter implements ItemSimilarityPersistencePort {

    private final ItemSimilarityRepository jpa;
    private final RecommendationSignalDomainMapper mapper;

    @Override
    public List<ItemSimilarity> findNeighbours(String productId, int limit) {
        return jpa.findNeighbours(productId, Math.max(1, limit)).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ItemSimilarity> findNeighbours(Collection<String> productIds, int limitPerProduct) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return jpa.findNeighboursForAll(
                        productIds.toArray(String[]::new), Math.max(1, limitPerProduct))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void upsertAll(List<ItemSimilarity> similarities) {
        if (similarities == null || similarities.isEmpty()) {
            return;
        }
        for (ItemSimilarity similarity : similarities) {
            jpa.upsert(
                    similarity.productId(),
                    similarity.similarProductId(),
                    similarity.score().value(),
                    similarity.coOccurrence(),
                    similarity.computedAt());
        }
    }

    @Override
    public int deleteComputedBefore(Instant cutoff) {
        return jpa.deleteComputedBefore(cutoff);
    }
}
