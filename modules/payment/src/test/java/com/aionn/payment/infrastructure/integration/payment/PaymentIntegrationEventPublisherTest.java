package com.aionn.payment.infrastructure.integration.payment;

import com.aionn.sharedkernel.integration.event.payment.PaymentCapturedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.payment.PaymentFailedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.payment.PaymentRefundedIntegrationEvent;
import com.aionn.sharedkernel.integration.publisher.IntegrationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PaymentIntegrationEventPublisherTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private IntegrationEventPublisher eventPublisher;
    private PaymentIntegrationEventPublisher publisher;

    @BeforeEach
    void setUp() {
        eventPublisher = mock(IntegrationEventPublisher.class);
        publisher = new PaymentIntegrationEventPublisher(eventPublisher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void capturedEventUsesInjectedClock() {
        publisher.publishPaymentCaptured("payment-1", "order-1", "transaction-1", BigDecimal.TEN, "VND");

        ArgumentCaptor<PaymentCapturedIntegrationEvent> event =
                ArgumentCaptor.forClass(PaymentCapturedIntegrationEvent.class);
        verify(eventPublisher).publish(event.capture());
        assertThat(event.getValue().occurredAt()).isEqualTo(NOW);
    }

    @Test
    void failedEventUsesInjectedClock() {
        publisher.publishPaymentFailed("payment-1", "order-1", "DECLINED", "Declined");

        ArgumentCaptor<PaymentFailedIntegrationEvent> event =
                ArgumentCaptor.forClass(PaymentFailedIntegrationEvent.class);
        verify(eventPublisher).publish(event.capture());
        assertThat(event.getValue().occurredAt()).isEqualTo(NOW);
    }

    @Test
    void refundedEventUsesInjectedClock() {
        publisher.publishPaymentRefunded(
                "payment-1", "order-1", "refund-1", BigDecimal.ONE, "VND", "Customer request");

        ArgumentCaptor<PaymentRefundedIntegrationEvent> event =
                ArgumentCaptor.forClass(PaymentRefundedIntegrationEvent.class);
        verify(eventPublisher).publish(event.capture());
        assertThat(event.getValue().occurredAt()).isEqualTo(NOW);
    }
}
