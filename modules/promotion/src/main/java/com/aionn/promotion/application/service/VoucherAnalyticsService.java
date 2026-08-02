package com.aionn.promotion.application.service;

import com.aionn.promotion.application.dto.analytics.result.MerchantVoucherAnalyticsResult;
import com.aionn.promotion.application.port.out.VoucherPersistencePort;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoucherAnalyticsService {

    private final VoucherPersistencePort voucherRepository;
    private final MerchantQueryPort merchantQueryPort;

    @Transactional(readOnly = true)
    public MerchantVoucherAnalyticsResult getMerchantAnalytics(String ownerId) {
        String merchantId = merchantQueryPort.findMerchantIdByOwnerId(ownerId)
                .orElseThrow(() -> new IllegalStateException("No merchant for owner " + ownerId));
        var rows = voucherRepository.findMerchantVoucherRows(merchantId);

        long totalIssued = 0;
        long totalRedeemed = 0;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        for (var row : rows) {
            int limit = row.usageLimit();
            int used = row.usedCount();
            totalIssued += limit;
            totalRedeemed += used;
            if (row.discountAmount() != null && used > 0) {
                totalDiscount = totalDiscount.add(row.discountAmount().multiply(BigDecimal.valueOf(used)));
            }
        }
        long remaining = Math.max(0, totalIssued - totalRedeemed);
        double rate = totalIssued == 0 ? 0.0 : (double) totalRedeemed / totalIssued;
        List<MerchantVoucherAnalyticsResult.TopVoucher> top = rows.stream()
                .map(row -> new MerchantVoucherAnalyticsResult.TopVoucher(
                        row.voucherCode(),
                        row.campaignId(),
                        row.usedCount(),
                        row.usageLimit(),
                        row.discountAmount() == null ? BigDecimal.ZERO : row.discountAmount()))
                .sorted(Comparator.comparingLong(MerchantVoucherAnalyticsResult.TopVoucher::redeemed).reversed())
                .limit(10)
                .toList();
        return new MerchantVoucherAnalyticsResult(totalIssued, totalRedeemed, remaining, rate, totalDiscount, top);
    }
}
