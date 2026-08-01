package com.aionn.payment.infrastructure.provider;

import com.aionn.payment.application.port.out.PaymentProviderClient;
import com.aionn.payment.application.port.out.observability.PaymentMetricsPort;
import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.function.Supplier;

@Slf4j
public class ResilientPaymentProviderClient implements PaymentProviderClient {

    private final PaymentProviderClient delegate;
    private final RetryRegistry retryRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final PaymentMetricsPort metrics;
    private final String gatewayLabel;

    public ResilientPaymentProviderClient(
            PaymentProviderClient delegate,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            PaymentMetricsPort metrics) {
        this.delegate = delegate;
        this.retryRegistry = retryRegistry;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.metrics = metrics;
        this.gatewayLabel = delegate.kind().name().toLowerCase();
    }

    @Override
    public PaymentGatewayKind kind() {
        return delegate.kind();
    }

    @Override
    public Authorization authorize(AuthorizationRequest request) {
        return execute("authorize", true, () -> delegate.authorize(request));
    }

    @Override
    public Refund refund(RefundRequest request) {
        return execute("refund", false, () -> delegate.refund(request));
    }

    @Override
    public String generateInvoice(String paymentId, String orderId, BigDecimal amount, String currency) {
        return execute("generateInvoice", false,
                () -> delegate.generateInvoice(paymentId, orderId, amount, currency));
    }

    @Override
    public WebhookEvent verifyAndParse(String rawBody, String signatureHeader) {
        return execute("verifyWebhook", false, () -> delegate.verifyAndParse(rawBody, signatureHeader));
    }

    private <T> T execute(String operation, boolean retryable, Supplier<T> action) {
        String instance = "payment-provider-" + gatewayLabel + "-" + operation;
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(instance);
        Supplier<T> decorated = CircuitBreaker.decorateSupplier(circuitBreaker, action);
        if (retryable) {
            Retry retry = retryRegistry.retry(instance);
            decorated = Retry.decorateSupplier(retry, decorated);
        }
        try {
            T result = decorated.get();
            metrics.providerOutcome(gatewayLabel, operation, "success");
            return result;
        } catch (RuntimeException ex) {
            metrics.providerOutcome(gatewayLabel, operation, "failure");
            log.error("Payment provider {} {} failed: {}", gatewayLabel, operation, ex.getMessage());
            throw ex;
        }
    }
}
