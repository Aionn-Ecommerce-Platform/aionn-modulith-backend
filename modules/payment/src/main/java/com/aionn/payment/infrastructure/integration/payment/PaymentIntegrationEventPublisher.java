package com.aionn.payment.infrastructure.integration.payment;

import com.aionn.payment.application.port.out.integration.PaymentIntegrationEventPublisherPort;
import com.aionn.sharedkernel.integration.event.payment.PaymentCapturedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.payment.PaymentFailedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.payment.PaymentRefundedIntegrationEvent;
import com.aionn.sharedkernel.integration.publisher.IntegrationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class PaymentIntegrationEventPublisher implements PaymentIntegrationEventPublisherPort {

    private final IntegrationEventPublisher integrationEventPublisher;

    @Override
    public void publishPaymentCaptured(String paymentId, String orderId, String transactionNo,
            BigDecimal amount, String currency) {
        integrationEventPublisher.publish(new PaymentCapturedIntegrationEvent(
                null, paymentId, orderId, transactionNo, amount, currency, Instant.now()));
    }

    @Override
    public void publishPaymentFailed(String paymentId, String orderId, String errorCode, String reason) {
        integrationEventPublisher.publish(new PaymentFailedIntegrationEvent(
                null, paymentId, orderId, errorCode, reason, Instant.now()));
    }

    @Override
    public void publishPaymentRefunded(String paymentId, String orderId, String refundTransactionNo,
            BigDecimal amount, String currency, String reason) {
        integrationEventPublisher.publish(new PaymentRefundedIntegrationEvent(
                null, paymentId, orderId, refundTransactionNo, amount, currency, reason, Instant.now()));
    }
}
