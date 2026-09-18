package com.aionn.ucp.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Domain record representing an active or completed UCP checkout session.
 */
public record UcpCheckoutSession(
        String id,
        String userId,
        String cartId,
        String status,
        String currency,
        Map<String, Integer> items,
        Map<String, Object> buyer,
        Map<String, Object> context,
        String orderId,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt) {

    public UcpCheckoutSession {
        items = items != null ? Collections.unmodifiableMap(new LinkedHashMap<>(items)) : Map.of();
        buyer = buyer != null ? Collections.unmodifiableMap(new LinkedHashMap<>(buyer)) : null;
        context = context != null ? Collections.unmodifiableMap(new LinkedHashMap<>(context)) : null;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    public boolean isCompleted() {
        return "completed".equalsIgnoreCase(status);
    }

    public boolean isCanceled() {
        return "canceled".equalsIgnoreCase(status);
    }

    public UcpCheckoutSession withUpdatedItems(Map<String, Integer> newItems, Map<String, Object> newBuyer,
            Map<String, Object> newContext, String newCurrency, Instant now) {
        return new UcpCheckoutSession(
                id,
                userId,
                cartId,
                "incomplete",
                newCurrency != null ? newCurrency : currency,
                newItems,
                newBuyer != null ? newBuyer : buyer,
                newContext != null ? newContext : context,
                orderId,
                createdAt,
                now,
                expiresAt);
    }

    public UcpCheckoutSession withCompleted(String placedOrderId, Instant now) {
        return new UcpCheckoutSession(
                id,
                userId,
                cartId,
                "completed",
                currency,
                items,
                buyer,
                context,
                placedOrderId,
                createdAt,
                now,
                expiresAt);
    }

    public UcpCheckoutSession withCanceled(Instant now) {
        return new UcpCheckoutSession(
                id,
                userId,
                cartId,
                "canceled",
                currency,
                items,
                buyer,
                context,
                orderId,
                createdAt,
                now,
                expiresAt);
    }
}
