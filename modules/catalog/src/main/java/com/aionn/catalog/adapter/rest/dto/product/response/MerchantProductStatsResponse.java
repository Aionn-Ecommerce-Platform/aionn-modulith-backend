package com.aionn.catalog.adapter.rest.dto.product.response;

public record MerchantProductStatsResponse(
        long total,
        long published,
        long variants,
        long sold) {
}
