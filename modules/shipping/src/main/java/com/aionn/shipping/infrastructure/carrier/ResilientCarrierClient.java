package com.aionn.shipping.infrastructure.carrier;

import com.aionn.shipping.application.port.out.CarrierClient;
import com.aionn.shipping.application.port.out.observability.ShippingMetricsPort;
import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import com.aionn.shipping.domain.valueobject.ShipmentDimensions;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Component
@Primary
@Order(0)
public class ResilientCarrierClient implements CarrierClient {

    private static final String INSTANCE = "shipping-carrier";

    private final CarrierClient delegate;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final ShippingMetricsPort metrics;

    public ResilientCarrierClient(
            List<CarrierClient> delegates,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            ShippingMetricsPort metrics) {
        this.delegate = delegates.stream()
                .filter(impl -> !(impl instanceof ResilientCarrierClient))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No underlying CarrierClient implementation found"));
        this.retry = retryRegistry.retry(INSTANCE);
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(INSTANCE);
        this.metrics = metrics;
    }

    @Override
    public Quote quote(ShipmentAddress address, ShipmentDimensions dimensions, String currency) {
        return execute("quote", () -> delegate.quote(address, dimensions, currency));
    }

    @Override
    public Registration register(String shipmentId, String orderId, ShipmentAddress address,
            ShipmentDimensions dimensions, BigDecimal codAmount, BigDecimal shippingFee, String currency) {
        return execute("register",
                () -> delegate.register(shipmentId, orderId, address, dimensions, codAmount, shippingFee,
                        currency));
    }

    @Override
    public String fetchLabel(String trackingCode) {
        return execute("fetchLabel", () -> delegate.fetchLabel(trackingCode));
    }

    @Override
    public void cancel(String trackingCode, String reason) {
        execute("cancel", () -> {
            delegate.cancel(trackingCode, reason);
            return null;
        });
    }

    @Override
    public CarrierClient.OrderDetail fetchOrderDetail(String trackingCode) {
        return execute("fetchOrderDetail", () -> delegate.fetchOrderDetail(trackingCode));
    }

    private <T> T execute(String operation, Supplier<T> action) {
        Supplier<T> decorated = Retry.decorateSupplier(retry,
                CircuitBreaker.decorateSupplier(circuitBreaker, action));
        try {
            T result = decorated.get();
            metrics.carrierOutcome(operation, "success");
            return result;
        } catch (RuntimeException ex) {
            metrics.carrierOutcome(operation, "failure");
            log.error("Carrier {} failed: {}", operation, ex.getMessage());
            throw ex;
        }
    }
}
