package com.aionn.ordering.infrastructure.integration.shipping;

import java.math.BigDecimal;
import java.util.List;

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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Slf4j
@Component
@Primary
@Order(0)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ResilientShippingAdapter implements ShippingGateway {

    private static final String INSTANCE = "ordering-shipping";

    private final ShippingGateway delegate;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final OrderingMetricsPort metrics;

    public ResilientShippingAdapter(
            ShippingFulfillmentAdapter delegate,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            OrderingMetricsPort metrics) {
        this.delegate = delegate;
        this.retry = retryRegistry.retry(INSTANCE);
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(INSTANCE);
        this.metrics = metrics;
    }

    @Override
    public ShippingGateway.ShippingQuote quote(String orderId, String merchantId, ShippingAddress address,
            String currency, List<ShippingGateway.Parcel> parcels,
            BigDecimal goodsSubtotal) {
        return execute("quote", () -> delegate.quote(orderId, merchantId, address, currency, parcels, goodsSubtotal));
    }

    @Override
    public ShippingGateway.ShippingQuote quote(String orderId, String merchantId, ShippingAddress address,
            String currency, List<ShippingGateway.Parcel> parcels,
            BigDecimal goodsSubtotal, String serviceCode) {
        return execute("quote", () -> delegate.quote(orderId, merchantId, address, currency, parcels,
                goodsSubtotal, serviceCode));
    }

    @Override
    public List<ShippingGateway.Registration> createAndRegister(String orderId, String merchantId,
            String userId, ShippingAddress address, BigDecimal codAmount,
            BigDecimal shippingFee, String currency, List<ShippingGateway.Parcel> parcels) {
        return execute("createAndRegister", () -> delegate.createAndRegister(orderId, merchantId, userId,
                address, codAmount, shippingFee, currency, parcels));
    }

    @Override
    public boolean hasPickedUpShipment(String orderId) {
        return execute("hasPickedUpShipment", () -> delegate.hasPickedUpShipment(orderId));
    }

    @Override
    public boolean hasActiveShipment(String orderId) {
        return execute("hasActiveShipment", () -> delegate.hasActiveShipment(orderId));
    }

    @Override
    public boolean hasUnregisteredShipment(String orderId) {
        return execute("hasUnregisteredShipment", () -> delegate.hasUnregisteredShipment(orderId));
    }

    @Override
    public boolean allShipmentsDelivered(String orderId) {
        return execute("allShipmentsDelivered", () -> delegate.allShipmentsDelivered(orderId));
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
