package com.aionn.promotion.application.port.in.voucher;

import com.aionn.promotion.application.dto.voucher.result.VoucherResult;
import com.aionn.promotion.domain.valueobject.VoucherType;

import java.util.List;

public interface ListShopVouchersByMerchantInputPort {
    List<VoucherResult> execute(String merchantId, int limit);

    List<VoucherResult> execute(String merchantId, int limit, VoucherType type);

    List<VoucherResult> execute(String merchantId, int limit, VoucherType type, boolean availableOnly);
}
