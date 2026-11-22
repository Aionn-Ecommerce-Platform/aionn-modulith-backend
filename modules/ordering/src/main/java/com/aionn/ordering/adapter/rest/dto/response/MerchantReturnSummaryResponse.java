package com.aionn.ordering.adapter.rest.dto.response;

import java.math.BigDecimal;

public record MerchantReturnSummaryResponse(
        long requests,
        long approved,
        BigDecimal refundTotal) {
}
