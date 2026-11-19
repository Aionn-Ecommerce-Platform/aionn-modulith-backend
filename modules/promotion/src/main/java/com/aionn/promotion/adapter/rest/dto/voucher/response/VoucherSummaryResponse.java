package com.aionn.promotion.adapter.rest.dto.voucher.response;

public record VoucherSummaryResponse(
        long all,
        long platform,
        long shop,
        long shipping) {
}
