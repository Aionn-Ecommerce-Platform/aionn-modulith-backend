package com.aionn.catalog.infrastructure.policy;

import com.aionn.catalog.application.policy.CatalogProductPolicy;
import com.aionn.catalog.infrastructure.config.properties.CatalogProductProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringCatalogProductPolicy implements CatalogProductPolicy {

    private final CatalogProductProperties properties;

    @Override
    public int getBulkPriceUpdateMaxSize() {
        return properties.bulkPriceUpdateMaxSize();
    }
}
