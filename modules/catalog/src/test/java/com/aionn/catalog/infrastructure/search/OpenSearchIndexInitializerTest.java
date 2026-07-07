package com.aionn.catalog.infrastructure.search;

import com.aionn.catalog.infrastructure.config.properties.CatalogSearchProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.client.transport.endpoints.BooleanResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenSearchIndexInitializerTest {

    private OpenSearchClient client;
    private OpenSearchIndicesClient indices;
    private OpenSearchIndexInitializer initializer;

    @BeforeEach
    void setUp() {
        client = mock(OpenSearchClient.class);
        indices = mock(OpenSearchIndicesClient.class);
        when(client.indices()).thenReturn(indices);
        CatalogSearchProperties props = new CatalogSearchProperties("opensearch",
                new CatalogSearchProperties.OpenSearch("localhost", 9200, "http", "catalog-products", "", ""));
        initializer = new OpenSearchIndexInitializer(client, props);
    }

    @Test
    void skipsCreationWhenIndexExists() throws Exception {
        when(indices.exists(any(ExistsRequest.class))).thenReturn(new BooleanResponse(true));

        initializer.ensureIndexExists();

        verify(indices, never()).create(any(CreateIndexRequest.class));
    }

    @Test
    void createsIndexWhenMissing() throws Exception {
        when(indices.exists(any(ExistsRequest.class))).thenReturn(new BooleanResponse(false));

        initializer.ensureIndexExists();

        verify(indices).create(any(CreateIndexRequest.class));
    }

    @Test
    void swallowsClientErrors() throws Exception {
        when(indices.exists(any(ExistsRequest.class)))
                .thenThrow(new OpenSearchException(
                        new org.opensearch.client.opensearch._types.ErrorResponse.Builder()
                                .status(500)
                                .error(e -> e.type("boom").reason("down"))
                                .build()));

        initializer.ensureIndexExists();

        verify(indices, never()).create(any(CreateIndexRequest.class));
    }
}
