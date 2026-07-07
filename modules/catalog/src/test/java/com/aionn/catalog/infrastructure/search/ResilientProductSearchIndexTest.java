package com.aionn.catalog.infrastructure.search;

import com.aionn.catalog.application.dto.search.ProductSearchCriteria;
import com.aionn.catalog.application.dto.search.ProductSearchDocument;
import com.aionn.catalog.application.port.out.search.ProductSearchIndex;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResilientProductSearchIndexTest {

    private ProductSearchIndex delegate;
    private ResilientProductSearchIndex resilient;

    private static ProductSearchDocument doc() {
        return new ProductSearchDocument("p1", "m1", "Widget", null, null, List.of(), List.of(),
                List.of(), List.of(), Map.of(), null, null, null, "PUBLISHED", Instant.now(), 0.0, 0L);
    }

    @BeforeEach
    void setUp() {
        delegate = mock(ProductSearchIndex.class);
        resilient = new ResilientProductSearchIndex(
                List.of(delegate),
                RetryRegistry.ofDefaults(),
                CircuitBreakerRegistry.ofDefaults(),
                new SimpleMeterRegistry());
    }

    @Test
    void indexDelegates() {
        ProductSearchDocument document = doc();
        resilient.index(document);
        verify(delegate).index(document);
    }

    @Test
    void indexSwallowsDelegateFailure() {
        ProductSearchDocument document = doc();
        doThrow(new RuntimeException("boom")).when(delegate).index(document);

        resilient.index(document);

        verify(delegate, atLeastOnce()).index(document);
    }

    @Test
    void indexAllAndRemoveAllSkipEmpty() {
        resilient.indexAll(List.of());
        resilient.removeAll(List.of());
        verify(delegate, never()).indexAll(any());
        verify(delegate, never()).removeAll(any());
    }

    @Test
    void removeDelegates() {
        resilient.remove("p1");
        verify(delegate).remove("p1");
    }

    @Test
    void searchDelegatesResult() {
        ProductSearchCriteria criteria = new ProductSearchCriteria(null, null, null, List.of(), List.of(),
                null, null, Map.of(), ProductSearchCriteria.Sort.RELEVANCE, 0, 20);
        ProductSearchIndex.SearchHits hits = new ProductSearchIndex.SearchHits(
                List.of("p1"), 1L, Map.of(), Map.of(), Map.of(), null, null);
        when(delegate.search(criteria)).thenReturn(Optional.of(hits));

        assertThat(resilient.search(criteria)).contains(hits);
    }

    @Test
    void searchReturnsEmptyOnFailure() {
        ProductSearchCriteria criteria = new ProductSearchCriteria(null, null, null, List.of(), List.of(),
                null, null, Map.of(), ProductSearchCriteria.Sort.RELEVANCE, 0, 20);
        when(delegate.search(criteria)).thenThrow(new RuntimeException("down"));

        assertThat(resilient.search(criteria)).isEmpty();
    }
}
