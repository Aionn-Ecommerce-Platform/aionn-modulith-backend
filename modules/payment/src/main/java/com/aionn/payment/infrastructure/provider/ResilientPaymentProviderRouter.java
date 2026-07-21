package com.aionn.payment.infrastructure.provider;

import com.aionn.payment.application.port.out.PaymentProviderClient;
import com.aionn.payment.application.port.out.PaymentProviderRouter;
import com.aionn.payment.application.port.out.observability.PaymentMetricsPort;
import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
@Primary
@Order(0)
public class ResilientPaymentProviderRouter implements PaymentProviderRouter {

    private final PaymentProviderRouter delegate;
    private final RetryRegistry retryRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final PaymentMetricsPort metrics;
    private final Map<PaymentGatewayKind, PaymentProviderClient> wrapped = new EnumMap<>(PaymentGatewayKind.class);

    public ResilientPaymentProviderRouter(
            DefaultPaymentProviderRouter delegate,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            PaymentMetricsPort metrics) {
        this.delegate = delegate;
        this.retryRegistry = retryRegistry;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.metrics = metrics;
    }

    @Override
    public PaymentProviderClient route(PaymentGatewayKind kind) {
        return wrapped.computeIfAbsent(kind, k -> new ResilientPaymentProviderClient(
                delegate.route(k), retryRegistry, circuitBreakerRegistry, metrics));
    }
}
