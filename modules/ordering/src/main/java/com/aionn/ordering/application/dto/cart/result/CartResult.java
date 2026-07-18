package com.aionn.ordering.application.dto.cart.result;

import java.time.Instant;
import java.util.List;

public record CartResult(
        String cartId,
        String userId,
        List<CartItemResult> items,
        String voucherCode,
        Instant createdAt,
        Instant updatedAt) {

    public record CartItemResult(String skuId, int qty) {
    }
}

