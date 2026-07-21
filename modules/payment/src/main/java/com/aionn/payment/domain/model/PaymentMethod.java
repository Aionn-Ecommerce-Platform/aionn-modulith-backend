package com.aionn.payment.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.payment.domain.event.PaymentMethodEvents;
import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.valueobject.PaymentMethodStatus;
import lombok.Getter;

import java.time.Instant;

@Getter
public class PaymentMethod extends AggregateRoot {

    private final String methodId;
    private final String userId;
    private final String provider;
    private final String last4Digits;
    private final String gatewayToken;
    private PaymentMethodStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant verifiedAt;

    public PaymentMethod(
            String methodId,
            String userId,
            String provider,
            String last4Digits,
            String gatewayToken,
            PaymentMethodStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant verifiedAt) {
        this.methodId = methodId;
        this.userId = userId;
        this.provider = provider;
        this.last4Digits = last4Digits;
        this.gatewayToken = gatewayToken;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.verifiedAt = verifiedAt;
    }

    public static PaymentMethod link(
            String methodId, String userId, String provider, String last4Digits, String gatewayToken, Instant now) {
        Guard.require(gatewayToken != null && !gatewayToken.isBlank(),
                () -> new PaymentException(PaymentErrorCode.INVALID_ARGUMENT, "gatewayToken required"));
        PaymentMethod m = new PaymentMethod(methodId, userId, provider, last4Digits, gatewayToken,
                PaymentMethodStatus.LINKED, now, now, null);
        m.registerEvent(new PaymentMethodEvents.PaymentMethodLinked(methodId, userId, provider, last4Digits, now));
        return m;
    }

    public void ensureOwnedBy(String userId) {
        Guard.require(this.userId.equals(userId),
                () -> new PaymentException(PaymentErrorCode.METHOD_FORBIDDEN));
    }

    public void verify(Instant now) {
        ensureTransition(PaymentMethodStatus.VERIFIED);
        this.status = PaymentMethodStatus.VERIFIED;
        this.verifiedAt = now;
        this.updatedAt = now;
        registerEvent(new PaymentMethodEvents.PaymentMethodVerified(methodId, userId, verifiedAt, now));
    }

    public void remove(Instant now) {
        if (status == PaymentMethodStatus.REMOVED) {
            return;
        }
        ensureTransition(PaymentMethodStatus.REMOVED);
        this.status = PaymentMethodStatus.REMOVED;
        this.updatedAt = now;
        registerEvent(new PaymentMethodEvents.PaymentMethodRemoved(methodId, userId, now));
    }

    private void ensureTransition(PaymentMethodStatus next) {
        Guard.require(status.canTransitionTo(next),
                () -> new PaymentException(PaymentErrorCode.PAYMENT_INVALID_STATE,
                        "Cannot transition payment method from " + status + " to " + next));
    }

    @Override
    protected String aggregateId() {
        return methodId;
    }
}
