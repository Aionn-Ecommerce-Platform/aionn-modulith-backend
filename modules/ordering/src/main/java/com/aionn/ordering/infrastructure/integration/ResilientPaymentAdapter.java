package com.aionn.ordering.infrastructure.integration;

import com.aionn.ordering.application.port.out.observability.OrderingMetricsPort;
import com.aionn.ordering.application.port.out.PaymentGateway;
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
public class ResilientPaymentAdapter implements PaymentGateway {

    private static final String INSTANCE = "ordering-payment";

    private final PaymentGateway delegate;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final OrderingMetricsPort metrics;

    public ResilientPaymentAdapter(
            List<PaymentGateway> delegates,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            OrderingMetricsPort metrics) {
        this.delegate = delegates.stream()
                .filter(impl -> !(impl instanceof ResilientPaymentAdapter))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No underlying PaymentGateway implementation found"));
        this.retry = retryRegistry.retry(INSTANCE);
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(INSTANCE);
        this.metrics = metrics;
    }

    @Override
    public PaymentGateway.PaymentAuthorization authorize(String orderId, String userId, String paymentMethodId,
            BigDecimal amount, String currency, String gateway) {
        return execute("authorize",
                () -> delegate.authorize(orderId, userId, paymentMethodId, amount, currency, gateway));
    }

    @Override
    public void refund(String paymentId, BigDecimal amount, String currency, String reason) {
        execute("refund", () -> {
            delegate.refund(paymentId, amount, currency, reason);
            return null;
        });
    }

    private <T> T execute(String operation, Supplier<T> action) {
        Supplier<T> decorated = Retry.decorateSupplier(retry,
                CircuitBreaker.decorateSupplier(circuitBreaker, action));
        try {
            T result = decorated.get();
            metrics.gatewayOutcome("payment", "success");
            return result;
        } catch (RuntimeException ex) {
            metrics.gatewayOutcome("payment", "failure");
            log.error("Payment gateway {} failed: {}", operation, ex.getMessage());
            throw ex;
        }
    }
}
