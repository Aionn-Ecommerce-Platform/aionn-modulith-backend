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

    private static final String INSTANCE = "payment-provider";

    private final PaymentProviderClient delegate;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final PaymentMetricsPort metrics;
    private final String gatewayLabel;

    public ResilientPaymentProviderClient(
            PaymentProviderClient delegate,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            PaymentMetricsPort metrics) {
        this.delegate = delegate;
        this.retry = retryRegistry.retry(INSTANCE);
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(INSTANCE);
        this.metrics = metrics;
        this.gatewayLabel = delegate.kind().name().toLowerCase();
    }

    @Override
    public PaymentGatewayKind kind() {
        return delegate.kind();
    }

    @Override
    public Authorization authorize(AuthorizationRequest request) {
        return execute("authorize", () -> delegate.authorize(request));
    }

    @Override
    public Refund refund(RefundRequest request) {
        return execute("refund", () -> delegate.refund(request));
    }

    @Override
    public String generateInvoice(String paymentId, String orderId, BigDecimal amount, String currency) {
        return execute("generateInvoice",
                () -> delegate.generateInvoice(paymentId, orderId, amount, currency));
    }

    @Override
    public WebhookEvent verifyAndParse(String rawBody, String signatureHeader) {
        return execute("verifyWebhook", () -> delegate.verifyAndParse(rawBody, signatureHeader));
    }

    private <T> T execute(String operation, Supplier<T> action) {
        Supplier<T> decorated = Retry.decorateSupplier(retry,
                CircuitBreaker.decorateSupplier(circuitBreaker, action));
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
