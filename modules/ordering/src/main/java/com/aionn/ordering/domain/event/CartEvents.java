package com.aionn.ordering.domain.event;

import java.time.Instant;

public final class CartEvents {

    private CartEvents() {
    }

    public record ItemAddedToCart(
            String cartId, String userId, String skuId, int qty, Instant occurredAt) implements OrderingEvent {
    }

    public record CartItemUpdated(
            String cartId, String skuId, int newQty, Instant occurredAt) implements OrderingEvent {
    }

    public record CartItemRemoved(
            String cartId, String skuId, Instant occurredAt) implements OrderingEvent {
    }

    public record CartCleared(
            String cartId, String userId, String reason, Instant occurredAt) implements OrderingEvent {
    }

    public record VoucherApplied(
            String cartId, String voucherCode, Instant occurredAt) implements OrderingEvent {
    }

    public record VoucherRemoved(
            String cartId, Instant occurredAt) implements OrderingEvent {
    }
}

