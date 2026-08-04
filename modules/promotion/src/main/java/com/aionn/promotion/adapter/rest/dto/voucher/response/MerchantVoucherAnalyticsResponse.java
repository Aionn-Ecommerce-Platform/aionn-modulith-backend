package com.aionn.promotion.adapter.rest.dto.voucher.response;

import java.math.BigDecimal;
import java.util.List;

public record MerchantVoucherAnalyticsResponse(
        long totalIssued, long totalRedeemed, long totalRemaining,
        double redemptionRate, BigDecimal totalDiscountValue,
        List<TopVoucher> topVouchers) {
    public record TopVoucher(
            String voucherCode, String campaignId, long redeemed,
            long usageLimit, BigDecimal discountAmount) {}
}
