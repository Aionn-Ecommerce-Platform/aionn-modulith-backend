package com.aionn.recommendation.infrastructure.persistence.repository;

import com.aionn.recommendation.infrastructure.persistence.entity.ProductPopularityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Reads over the popularity table. Writes go through {@code JdbcTemplate} in the persistence adapter:
 * the refresh rewrites the whole active set, and batching there keeps the rebuild inside its
 * transaction budget instead of issuing one round trip per product.
 */
public interface ProductPopularityRepository
        extends JpaRepository<ProductPopularityEntity, String> {

    @Query(value = """
            SELECT * FROM recommendation_popularity
            ORDER BY popularity_score DESC, product_id
            LIMIT :limit
            """, nativeQuery = true)
    List<ProductPopularityEntity> findTop(@Param("limit") int limit);

    List<ProductPopularityEntity> findByProductIdIn(Collection<String> productIds);
}
