package com.aionn.promotion.application.port.out;

import com.aionn.promotion.domain.model.Voucher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface VoucherPersistencePort {

    Voucher save(Voucher voucher);

    Optional<Voucher> findByCode(String voucherCode);

    Optional<Voucher> lockByCode(String voucherCode);

    List<Voucher> findByCampaignId(String campaignId, int limit);

    List<Voucher> findByMerchantId(String merchantId, int limit);

    List<MerchantVoucherRow> findMerchantVoucherRows(String merchantId);

    record MerchantVoucherRow(
            String voucherCode,
            String campaignId,
            int usageLimit,
            int usedCount,
            BigDecimal discountAmount) {
    }
}
