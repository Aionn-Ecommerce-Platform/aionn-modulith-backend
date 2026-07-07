package com.aionn.catalog.infrastructure.search;

import com.aionn.catalog.application.dto.search.ProductSearchCriteria;
import com.aionn.catalog.application.dto.search.ProductSearchDocument;
import com.aionn.catalog.infrastructure.config.properties.CatalogSearchProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenSearchProductSearchIndexTest {

    private OpenSearchClient client;
    private OpenSearchProductSearchIndex index;

    private static ProductSearchDocument doc() {
        return new ProductSearchDocument("p1", "m1", "Widget", "desc", "b1", List.of("c1"),
                List.of(), List.of("tag"), List.of(), Map.of("color", "red"),
                new BigDecimal("100"), new BigDecimal("100"), "VND", "PUBLISHED", Instant.now(), 4.0, 3L);
    }

    @BeforeEach
    void setUp() {
        client = mock(OpenSearchClient.class);
        CatalogSearchProperties props = new CatalogSearchProperties("opensearch",
                new CatalogSearchProperties.OpenSearch("localhost", 9200, "http", "catalog-products", "", ""));
        index = new OpenSearchProductSearchIndex(client, props);
    }

    @Test
    void indexSendsDocument() throws IOException {
        index.index(doc());
        verify(client)
                .index(org.mockito.ArgumentMatchers.<org.opensearch.client.opensearch.core.IndexRequest<Object>>any());
    }

    @Test
    void indexAllSkipsEmpty() throws IOException {
        index.indexAll(List.of());
        verify(client, org.mockito.Mockito.never()).bulk(any(BulkRequest.class));
    }

    @Test
    void indexAllSendsBulk() throws IOException {
        BulkResponse response = mock(BulkResponse.class);
        when(response.errors()).thenReturn(false);
        when(client.bulk(any(BulkRequest.class))).thenReturn(response);

        index.indexAll(List.of(doc()));

        verify(client).bulk(any(BulkRequest.class));
    }

    @Test
    void removeDeletesById() throws IOException {
        index.remove("p1");
        verify(client).delete(any(org.opensearch.client.opensearch.core.DeleteRequest.class));
    }

    @Test
    void indexWrapsIoException() throws IOException {
        when(client
                .index(org.mockito.ArgumentMatchers.<org.opensearch.client.opensearch.core.IndexRequest<Object>>any()))
                .thenThrow(new IOException("down"));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> index.index(doc()));
    }

    @Test
    void searchReturnsEmptyWhenClientFails() throws IOException {
        when(client.search(any(org.opensearch.client.opensearch.core.SearchRequest.class),
                org.mockito.ArgumentMatchers.eq(ProductSearchDocument.class)))
                .thenThrow(new IOException("cluster down"));

        ProductSearchCriteria criteria = new ProductSearchCriteria(
                "widget", "m1", null, List.of("c1"), List.of("b1"),
                new BigDecimal("10"), new BigDecimal("500"), Map.of("color", List.of("red")),
                ProductSearchCriteria.Sort.PRICE_ASC, 0, 20, 3.0);

        assertThat(index.search(criteria)).isEmpty();
    }
}
