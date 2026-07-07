package com.aionn.catalog.infrastructure.search;

import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OpenSearchConfigTest {

    @Test
    void buildsOpenSearchClient() {
        OpenSearchConfig config = new OpenSearchConfig();
        ReflectionTestUtils.setField(config, "host", "localhost");
        ReflectionTestUtils.setField(config, "port", 9200);
        ReflectionTestUtils.setField(config, "scheme", "http");

        OpenSearchClient client = config.openSearchClient();

        assertThat(client).isNotNull();
    }
}
