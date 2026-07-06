package com.aionn.catalog.infrastructure.policy;

import com.aionn.catalog.infrastructure.config.properties.CatalogProductProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpringCatalogProductPolicyTest {

    @Test
    void getBulkPriceUpdateMaxSizeReadsFromProperties() {
        CatalogProductProperties props = new CatalogProductProperties("VND", " (Copy)", 250, 200);
        SpringCatalogProductPolicy policy = new SpringCatalogProductPolicy(props);

        assertThat(policy.getBulkPriceUpdateMaxSize()).isEqualTo(250);
    }
}
