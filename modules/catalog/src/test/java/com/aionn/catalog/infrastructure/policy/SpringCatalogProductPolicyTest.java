package com.aionn.catalog.infrastructure.policy;

import com.aionn.catalog.infrastructure.config.properties.CatalogProductProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpringCatalogProductPolicyTest {

    @Test
    void exposesEachConcernFromTheSharedProperties() {
        CatalogProductProperties props = new CatalogProductProperties("VND", " (Copy)", 250, 200, 2500L);
        SpringCatalogProductPolicy policy = new SpringCatalogProductPolicy(props);

        assertThat(policy.maxUpdateSize()).isEqualTo(250);
        assertThat(policy.soldThreshold()).isEqualTo(2500L);
        assertThat(policy.batchSize()).isEqualTo(200);
        assertThat(policy.nameSuffix()).isEqualTo(" (Copy)");
    }
}
