package com.aionn.recommendation.application.port.out;

import com.aionn.recommendation.domain.model.ProductPopularity;

import java.util.Collection;
import java.util.List;

public interface PopularityPersistencePort {

    List<ProductPopularity> findTop(int limit);

    List<ProductPopularity> findByProductIds(Collection<String> productIds);

    void upsertAll(List<ProductPopularity> popularities);

    int deleteComputedBefore(java.time.Instant cutoff);
}
