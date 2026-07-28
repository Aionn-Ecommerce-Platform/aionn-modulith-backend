package com.aionn.chat.domain.valueobject;

import java.util.HashMap;
import java.util.Map;

public record MessagePayload(
        String body,
        Map<String, Object> metadata) {

    public MessagePayload {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static MessagePayload text(String body) {
        return new MessagePayload(body, Map.of());
    }

    public static MessagePayload image(String imageUrl, int width, int height, String thumbnailUrl) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("imageUrl", imageUrl);
        meta.put("width", width);
        meta.put("height", height);
        if (thumbnailUrl != null)
            meta.put("thumbnailUrl", thumbnailUrl);
        return new MessagePayload(null, meta);
    }

    public static MessagePayload productCard(
            String productId, String name, String imageUrl, String currency, String priceLabel) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("productId", productId);
        meta.put("name", name);
        meta.put("imageUrl", imageUrl);
        meta.put("currency", currency);
        meta.put("priceLabel", priceLabel);
        return new MessagePayload(null, meta);
    }

    public static MessagePayload orderRef(String orderId, String status, String totalLabel) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("orderId", orderId);
        meta.put("status", status);
        meta.put("totalLabel", totalLabel);
        return new MessagePayload(null, meta);
    }

    public static MessagePayload system(String body) {
        return new MessagePayload(body, Map.of("system", true));
    }
}

