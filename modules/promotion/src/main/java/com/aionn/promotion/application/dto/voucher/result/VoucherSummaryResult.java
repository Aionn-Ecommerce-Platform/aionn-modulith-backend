package com.aionn.promotion.application.dto.voucher.result;

public record VoucherSummaryResult(
        long all,
        long platform,
        long shop,
        long shipping) {
}
