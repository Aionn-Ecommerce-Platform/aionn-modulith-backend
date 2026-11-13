package com.aionn.catalog.infrastructure.policy;

import com.aionn.catalog.application.policy.BestSellerPolicy;
import com.aionn.catalog.application.policy.BulkPriceUpdatePolicy;
import com.aionn.catalog.application.policy.ProductClonePolicy;
import com.aionn.catalog.application.policy.SearchIndexBatchPolicy;
import com.aionn.catalog.infrastructure.config.properties.CatalogProductProperties;
import org.springframework.stereotype.Component;

/**
 * One adapter over {@code CatalogProductProperties}; the application-facing contracts stay narrow so
 * each service depends only on the rule it uses.
 */
@Component
public class SpringCatalogProductPolicy
        implements BulkPriceUpdatePolicy, BestSellerPolicy, SearchIndexBatchPolicy, ProductClonePolicy {

    private final CatalogProductProperties properties;

    public SpringCatalogProductPolicy(CatalogProductProperties properties) {
        this.properties = properties;
    }

    @Override
    public int maxUpdateSize() {
        return properties.bulkPriceUpdateMaxSize();
    }

    @Override
    public long soldThreshold() {
        return properties.bestSellerThreshold();
    }

    @Override
    public int batchSize() {
        return properties.reindexPageSize();
    }

    @Override
    public String nameSuffix() {
        return properties.cloneNameSuffix();
    }
}
