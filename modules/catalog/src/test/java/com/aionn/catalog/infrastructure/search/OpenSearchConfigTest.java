package com.aionn.catalog.infrastructure.search;

import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchClient;

import com.aionn.catalog.infrastructure.config.properties.CatalogSearchProperties;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class OpenSearchConfigTest {

    @Test
    void buildsOpenSearchClient() {
        CatalogSearchProperties properties = new CatalogSearchProperties("opensearch",
                new CatalogSearchProperties.OpenSearch("localhost", 9200, "http", "catalog-products", "", "",
                        Duration.ofSeconds(3), Duration.ofSeconds(10), 1, 0, 2, 15, 50, 100, 50));
        OpenSearchConfig config = new OpenSearchConfig(properties);

        OpenSearchClient client = config.openSearchClient();

        assertThat(client).isNotNull();
    }
}
