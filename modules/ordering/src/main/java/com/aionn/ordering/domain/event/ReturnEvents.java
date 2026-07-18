package com.aionn.ordering.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

public final class ReturnEvents {

    private ReturnEvents() {
    }

    public record ReturnRequested(
            String returnId, String orderId, String reason, String evidenceUrl, Instant occurredAt)
            implements OrderingEvent {
    }

    public record ReturnApproved(
            String returnId,
            String orderId,
            String merchantId,
            BigDecimal refundAmount,
            String currency,
            String returnWarehouseId,
            Instant approvedAt,
            Instant occurredAt) implements OrderingEvent {
    }

    public record ReturnItemReceived(
            String returnId,
            String orderId,
            String merchantId,
            String itemCondition,
            Instant receivedAt,
            Instant occurredAt) implements OrderingEvent {
    }

    public record ReturnRejected(
            String returnId,
            String orderId,
            String merchantId,
            String reason,
            Instant rejectedAt,
            Instant occurredAt) implements OrderingEvent {
    }
}

