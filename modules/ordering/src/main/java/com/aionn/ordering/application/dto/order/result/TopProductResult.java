package com.aionn.ordering.application.dto.order.result;

import java.math.BigDecimal;

public record TopProductResult(
        String skuId,
        long unitsSold,
        BigDecimal revenue) {
}
