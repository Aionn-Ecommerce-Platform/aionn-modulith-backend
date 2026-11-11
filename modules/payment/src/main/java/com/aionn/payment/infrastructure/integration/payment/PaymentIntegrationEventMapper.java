package com.aionn.payment.infrastructure.integration.payment;

import com.aionn.payment.domain.event.PaymentEvents;
import com.aionn.contracts.event.payment.PaymentFailedIntegrationEvent;
import com.aionn.contracts.event.payment.PaymentInitiatedIntegrationEvent;
import com.aionn.contracts.event.payment.PaymentPaidIntegrationEvent;
import com.aionn.contracts.event.payment.PaymentRefundedIntegrationEvent;
import org.springframework.stereotype.Component;

@Component
public class PaymentIntegrationEventMapper {

    public PaymentInitiatedIntegrationEvent toIntegrationEvent(PaymentEvents.PaymentInitiated domainEvent) {
        return new PaymentInitiatedIntegrationEvent(
                null,
                domainEvent.paymentId(),
                domainEvent.orderId(),
                domainEvent.amount(),
                domainEvent.currency(),
                domainEvent.gateway(),
                domainEvent.occurredAt());
    }

    public PaymentPaidIntegrationEvent toIntegrationEvent(PaymentEvents.PaymentProcessed domainEvent) {
        return new PaymentPaidIntegrationEvent(
                null,
                domainEvent.paymentId(),
                domainEvent.orderId(),
                domainEvent.amount(),
                domainEvent.currency(),
                domainEvent.gateway(),
                domainEvent.transactionNo(),
                domainEvent.occurredAt());
    }

    public PaymentFailedIntegrationEvent toIntegrationEvent(PaymentEvents.PaymentFailed domainEvent) {
        return new PaymentFailedIntegrationEvent(
                null,
                domainEvent.paymentId(),
                domainEvent.orderId(),
                domainEvent.errorCode(),
                domainEvent.reason(),
                domainEvent.occurredAt());
    }

    public PaymentRefundedIntegrationEvent toIntegrationEvent(PaymentEvents.PaymentRefunded domainEvent) {
        return new PaymentRefundedIntegrationEvent(
                null,
                domainEvent.paymentId(),
                domainEvent.orderId(),
                domainEvent.refundId(),
                domainEvent.amount(),
                domainEvent.currency(),
                domainEvent.reason(),
                domainEvent.occurredAt());
    }
}
