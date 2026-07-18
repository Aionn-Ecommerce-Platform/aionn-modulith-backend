package com.aionn.ordering.infrastructure.integration;

import com.aionn.ordering.application.port.out.CatalogPricingGateway;
import com.aionn.ordering.application.port.out.observability.OrderingMetricsPort;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Component
@Primary
@Order(0)
public class ResilientCatalogPricingAdapter implements CatalogPricingGateway {

    private static final String INSTANCE = "ordering-catalog";

    private final CatalogPricingGateway delegate;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final OrderingMetricsPort metrics;

    public ResilientCatalogPricingAdapter(
            List<CatalogPricingGateway> delegates,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            OrderingMetricsPort metrics) {
        this.delegate = delegates.stream()
                .filter(impl -> !(impl instanceof ResilientCatalogPricingAdapter))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No underlying CatalogPricingGateway implementation found"));
        this.retry = retryRegistry.retry(INSTANCE);
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(INSTANCE);
        this.metrics = metrics;
    }

    @Override
    public Map<String, CatalogPricingGateway.SkuPricing> resolve(List<String> skuIds) {
        Supplier<Map<String, CatalogPricingGateway.SkuPricing>> action = () -> delegate.resolve(skuIds);
        Supplier<Map<String, CatalogPricingGateway.SkuPricing>> decorated = Retry.decorateSupplier(retry,
                CircuitBreaker.decorateSupplier(circuitBreaker, action));
        try {
            Map<String, CatalogPricingGateway.SkuPricing> result = decorated.get();
            metrics.gatewayOutcome("catalog-pricing", "success");
            return result;
        } catch (RuntimeException ex) {
            metrics.gatewayOutcome("catalog-pricing", "failure");
            log.error("Catalog pricing gateway resolve failed: {}", ex.getMessage());
            throw ex;
        }
    }
}
