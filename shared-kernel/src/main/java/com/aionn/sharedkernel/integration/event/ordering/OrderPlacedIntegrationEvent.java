package com.aionn.sharedkernel.integration.event.ordering;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record OrderPlacedIntegrationEvent(
        String eventId,
        String orderId,
        String userId,
        String merchantId,
        String proposalId,
        List<OrderLineItem> items,
        BigDecimal totalAmount,
        String currency,
        String addressId,
        String paymentMethodId,
        Instant occurredAt) implements IntegrationEvent {

    public OrderPlacedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        items = IntegrationEvent.freezeList(items, "items");
        totalAmount = Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        currency = Objects.requireNonNull(currency, "currency must not be null");
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }

    public record OrderLineItem(
            String skuId,
            int quantity,
            BigDecimal unitPrice,
            String warehouseId,
            String reservationId) {
    }
}
