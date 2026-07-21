package com.aionn.ordering.infrastructure.integration.catalog;

import com.aionn.ordering.application.port.out.CatalogPricingGateway;
import com.aionn.ordering.application.port.out.observability.OrderingMetricsPort;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResilientCatalogPricingAdapterTest {

    @Mock
    private CatalogPricingGateway delegate;

    @Mock
    private RetryRegistry retryRegistry;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private Retry retry;

    private CircuitBreaker circuitBreaker;

    @Mock
    private OrderingMetricsPort metrics;

    private ResilientCatalogPricingAdapter adapter;

    @BeforeEach
    void setUp() {
        retry = io.github.resilience4j.retry.Retry.ofDefaults("ordering-catalog");
        circuitBreaker = io.github.resilience4j.circuitbreaker.CircuitBreaker.ofDefaults("ordering-catalog");

        when(retryRegistry.retry("ordering-catalog")).thenReturn(retry);
        when(circuitBreakerRegistry.circuitBreaker("ordering-catalog")).thenReturn(circuitBreaker);

        adapter = new ResilientCatalogPricingAdapter(List.of(delegate), retryRegistry, circuitBreakerRegistry, metrics);
    }

    @Test
    void resolveDelegatesAndRecordsMetricsOnSuccess() {
        CatalogPricingGateway.SkuPricing pricing = new CatalogPricingGateway.SkuPricing("sku-1", "m-1", "wh-1", BigDecimal.TEN, "VND", true);
        when(delegate.resolve(List.of("sku-1"))).thenReturn(Map.of("sku-1", pricing));

        Map<String, CatalogPricingGateway.SkuPricing> result = adapter.resolve(List.of("sku-1"));

        assertEquals(1, result.size());
        verify(metrics).gatewayOutcome("catalog-pricing", "success");
    }

    @Test
    void resolveRecordsMetricsOnFailure() {
        doThrow(new RuntimeException("Gateway error")).when(delegate).resolve(List.of("sku-1"));

        assertThrows(RuntimeException.class, () -> adapter.resolve(List.of("sku-1")));
        verify(metrics).gatewayOutcome("catalog-pricing", "failure");
    }
}
