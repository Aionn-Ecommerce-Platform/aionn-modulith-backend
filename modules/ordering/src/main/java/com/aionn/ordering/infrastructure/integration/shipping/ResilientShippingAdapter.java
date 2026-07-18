package com.aionn.ordering.infrastructure.integration.shipping;

import com.aionn.ordering.application.port.out.observability.OrderingMetricsPort;
import com.aionn.ordering.application.port.out.ShippingGateway;
import com.aionn.ordering.domain.valueobject.ShippingAddress;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Component
@Primary
@Order(0)
public class ResilientShippingAdapter implements ShippingGateway {

    private static final String INSTANCE = "ordering-shipping";

    private final ShippingGateway delegate;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final OrderingMetricsPort metrics;

    public ResilientShippingAdapter(
            List<ShippingGateway> delegates,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            OrderingMetricsPort metrics) {
        this.delegate = delegates.stream()
                .filter(impl -> !(impl instanceof ResilientShippingAdapter))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No underlying ShippingGateway implementation found"));
        this.retry = retryRegistry.retry(INSTANCE);
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(INSTANCE);
        this.metrics = metrics;
    }

    @Override
    public ShippingGateway.ShippingQuote quote(String orderId, String merchantId, ShippingAddress address, String currency) {
        return execute("quote", () -> delegate.quote(orderId, merchantId, address, currency));
    }

    @Override
    public ShippingGateway.Registration createAndRegister(String orderId, String merchantId, String userId,
            ShippingAddress address, java.math.BigDecimal codAmount, java.math.BigDecimal shippingFee, String currency) {
        return execute("createAndRegister", () -> delegate.createAndRegister(orderId, merchantId, userId,
                address, codAmount, shippingFee, currency));
    }

    private <T> T execute(String operation, Supplier<T> action) {
        Supplier<T> decorated = Retry.decorateSupplier(retry,
                CircuitBreaker.decorateSupplier(circuitBreaker, action));
        try {
            T result = decorated.get();
            metrics.gatewayOutcome("shipping", "success");
            return result;
        } catch (RuntimeException ex) {
            metrics.gatewayOutcome("shipping", "failure");
            log.error("Shipping gateway {} failed: {}", operation, ex.getMessage());
            throw ex;
        }
    }
}
