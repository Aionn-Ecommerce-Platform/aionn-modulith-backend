package com.aionn.recommendation.infrastructure.persistence.mapper;

import com.aionn.recommendation.domain.model.ItemSimilarity;
import com.aionn.recommendation.domain.model.ProductPopularity;
import com.aionn.recommendation.domain.valueobject.AffinityScore;
import com.aionn.recommendation.infrastructure.persistence.entity.ItemSimilarityEntity;
import com.aionn.recommendation.infrastructure.persistence.entity.ProductPopularityEntity;
import org.springframework.stereotype.Component;

@Component
public class RecommendationSignalDomainMapper {

    public ItemSimilarity toDomain(ItemSimilarityEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ItemSimilarity(
                entity.getId().getProductId(),
                entity.getId().getSimilarProductId(),
                AffinityScore.of(entity.getScore()),
                entity.getCoOccurrence(),
                entity.getComputedAt());
    }

    public ProductPopularity toDomain(ProductPopularityEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ProductPopularity(
                entity.getProductId(),
                entity.getPopularityScore(),
                entity.getViewCount(),
                entity.getPurchaseCount(),
                entity.getComputedAt());
    }
}
