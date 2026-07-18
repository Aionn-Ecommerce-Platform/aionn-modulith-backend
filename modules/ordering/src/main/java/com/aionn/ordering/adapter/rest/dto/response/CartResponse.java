package com.aionn.ordering.adapter.rest.dto.response;

import java.time.Instant;
import java.util.List;

public record CartResponse(
        String cartId,
        String userId,
        List<CartItemResponse> items,
        String voucherCode,
        Instant createdAt,
        Instant updatedAt) {

    public record CartItemResponse(String skuId, int qty) {
    }
}
