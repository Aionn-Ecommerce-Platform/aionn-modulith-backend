package com.aionn.catalog.infrastructure.search;

import com.aionn.catalog.application.dto.search.ProductSearchCriteria;
import com.aionn.catalog.application.dto.search.ProductSearchDocument;
import com.aionn.catalog.application.port.out.search.ProductSearchIndex;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

// Wraps the concrete search index with retry + circuit breaker so a flaky
// backend never propagates failures into business transactions. Write ops
// swallow failures; search falls back to Optional.empty for a JPA fallback.
@Slf4j
@Component
@Primary
@Order(0)
public class ResilientProductSearchIndex implements ProductSearchIndex {

    private static final String CIRCUIT_NAME = "catalog-search-index";
    private static final String RETRY_NAME = "catalog-search-index";

    private final ProductSearchIndex delegate;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final Counter failureCounter;
    private final Counter fallbackCounter;
    private final Counter successCounter;

    public ResilientProductSearchIndex(
            List<ProductSearchIndex> delegates,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            MeterRegistry meterRegistry) {
        this.delegate = delegates.stream()
                .filter(impl -> !(impl instanceof ResilientProductSearchIndex))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No underlying ProductSearchIndex implementation found"));
        this.retry = retryRegistry.retry(RETRY_NAME);
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_NAME);
        this.successCounter = meterRegistry.counter("catalog.search.index", "outcome", "success");
        this.failureCounter = meterRegistry.counter("catalog.search.index", "outcome", "failure");
        this.fallbackCounter = meterRegistry.counter("catalog.search.index", "outcome", "fallback");
    }

    @Override
    public void index(ProductSearchDocument document) {
        execute("index", document.productId(), () -> delegate.index(document));
    }

    @Override
    public void indexAll(List<ProductSearchDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        execute("indexAll", "size=" + documents.size(), () -> delegate.indexAll(documents));
    }

    @Override
    public void remove(String productId) {
        execute("remove", productId, () -> delegate.remove(productId));
    }

    @Override
    public void removeAll(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        execute("removeAll", "size=" + productIds.size(), () -> delegate.removeAll(productIds));
    }

    @Override
    public Optional<SearchHits> search(ProductSearchCriteria criteria) {
        try {
            return delegate.search(criteria);
        } catch (RuntimeException ex) {
            failureCounter.increment();
            log.warn("Search query failed; returning empty so caller can fall back: {}", rootCauseMessage(ex));
            return Optional.empty();
        }
    }

    private void execute(String operation, String correlation, Runnable action) {
        Runnable decorated = Retry.decorateRunnable(retry,
                CircuitBreaker.decorateRunnable(circuitBreaker, action));
        try {
            decorated.run();
            successCounter.increment();
        } catch (RuntimeException ex) {
            handleFailure(operation, correlation, ex);
        }
    }

    private void handleFailure(String operation, String correlation, RuntimeException ex) {
        failureCounter.increment();
        if (circuitBreaker.getState() == CircuitBreaker.State.OPEN
                || circuitBreaker.getState() == CircuitBreaker.State.FORCED_OPEN) {
            fallbackCounter.increment();
            log.warn("Search index circuit OPEN; skipping {} for {} (cause: {})",
                    operation, correlation, rootCauseMessage(ex));
            return;
        }
        log.error("Search index {} failed for {} after retries; swallowing to keep business tx green: {}",
                operation, correlation, rootCauseMessage(ex));
    }

    private static String rootCauseMessage(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}
