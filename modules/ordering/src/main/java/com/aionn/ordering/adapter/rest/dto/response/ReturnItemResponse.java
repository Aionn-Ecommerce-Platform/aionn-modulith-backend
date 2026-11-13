package com.aionn.ordering.adapter.rest.dto.response;

import java.math.BigDecimal;

public record ReturnItemResponse(
        String skuId,
        int qty,
        BigDecimal unitPrice,
        BigDecimal refundAmount) {
}
