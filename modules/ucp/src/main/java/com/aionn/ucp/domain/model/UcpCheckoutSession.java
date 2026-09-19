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
        Instant expiresAt,
        Map<String, Long> priceSnapshot,
        long version) {

    public UcpCheckoutSession {
        items = items != null ? Collections.unmodifiableMap(new LinkedHashMap<>(items)) : Map.of();
        buyer = buyer != null ? Collections.unmodifiableMap(new LinkedHashMap<>(buyer)) : null;
        context = context != null ? Collections.unmodifiableMap(new LinkedHashMap<>(context)) : null;
        priceSnapshot = priceSnapshot != null ? Collections.unmodifiableMap(new LinkedHashMap<>(priceSnapshot)) : Map.of();
    }

    public UcpCheckoutSession(
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
            Instant expiresAt,
            Map<String, Long> priceSnapshot) {
        this(id, userId, cartId, status, currency, items, buyer, context, orderId, createdAt, updatedAt, expiresAt, priceSnapshot, 1L);
    }

    public UcpCheckoutSession(
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
        this(id, userId, cartId, status, currency, items, buyer, context, orderId, createdAt, updatedAt, expiresAt, Map.of(), 1L);
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

    public boolean isCompleting() {
        return "completing".equalsIgnoreCase(status);
    }

    public UcpCheckoutSession withCompleting(Instant now) {
        return new UcpCheckoutSession(
                id,
                userId,
                cartId,
                "completing",
                currency,
                items,
                buyer,
                context,
                orderId,
                createdAt,
                now,
                expiresAt,
                priceSnapshot,
                version + 1);
    }

    public UcpCheckoutSession withUpdatedItems(Map<String, Integer> newItems, Map<String, Object> newBuyer,
            Map<String, Object> newContext, String newCurrency, Instant now) {
        return withUpdatedItems(newItems, newBuyer, newContext, newCurrency, now, priceSnapshot);
    }

    public UcpCheckoutSession withUpdatedItems(Map<String, Integer> newItems, Map<String, Object> newBuyer,
            Map<String, Object> newContext, String newCurrency, Instant now, Map<String, Long> newPriceSnapshot) {
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
                expiresAt,
                newPriceSnapshot != null ? newPriceSnapshot : priceSnapshot,
                version + 1);
    }

    public UcpCheckoutSession withCompleted(String placedOrderId, Instant now) {
        return withCompleted(placedOrderId, now, priceSnapshot);
    }

    public UcpCheckoutSession withCompleted(String placedOrderId, Instant now, Map<String, Long> completedPriceSnapshot) {
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
                expiresAt,
                completedPriceSnapshot != null ? completedPriceSnapshot : priceSnapshot,
                version + 1);
    }

    public UcpCheckoutSession withCanceled(Instant now) {
        return withCanceled(now, priceSnapshot);
    }

    public UcpCheckoutSession withCanceled(Instant now, Map<String, Long> canceledPriceSnapshot) {
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
                expiresAt,
                canceledPriceSnapshot != null ? canceledPriceSnapshot : priceSnapshot,
                version + 1);
    }
}
