package com.aionn.catalog.infrastructure.policy;

import com.aionn.catalog.application.policy.CatalogProductPolicy;
import com.aionn.catalog.infrastructure.config.properties.CatalogProductProperties;
import org.springframework.stereotype.Component;

@Component
public class SpringCatalogProductPolicy implements CatalogProductPolicy {

    private final CatalogProductProperties properties;

    public SpringCatalogProductPolicy(CatalogProductProperties properties) {
        this.properties = properties;
    }

    @Override
    public int getBulkPriceUpdateMaxSize() {
        return properties.bulkPriceUpdateMaxSize();
    }

    @Override
    public long getBestSellerThreshold() {
        return properties.bestSellerThreshold();
    }
}
