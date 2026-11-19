package com.aionn.promotion.application.port.out;

import com.aionn.promotion.domain.model.Voucher;
import com.aionn.promotion.domain.valueobject.VoucherType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface VoucherPersistencePort {

    Voucher save(Voucher voucher);

    Optional<Voucher> findByCode(String voucherCode);

    Optional<Voucher> lockByCode(String voucherCode);

    List<Voucher> findByCampaignId(String campaignId, int limit);

    List<Voucher> findByCampaignId(String campaignId, VoucherType type, int limit);

    List<Voucher> findByMerchantId(String merchantId, int limit);

    List<Voucher> findByMerchantId(String merchantId, VoucherType type, int limit);

    List<Voucher> findAvailableByCampaignId(String campaignId, VoucherType type, Instant now, int limit);

    List<Voucher> findAvailableByMerchantId(String merchantId, VoucherType type, Instant now, int limit);

    List<MerchantVoucherRow> findMerchantVoucherRows(String merchantId);

    VoucherSummary countAvailable(Instant now);

    record MerchantVoucherRow(
            String voucherCode,
            String campaignId,
            int usageLimit,
            int usedCount,
            BigDecimal discountAmount) {
    }

    record VoucherSummary(long platform, long shop, long shipping) {
    }
}
