package com.aionn.promotion.application.port.in.voucher;

import com.aionn.promotion.application.dto.voucher.result.VoucherResult;

import java.util.List;

public interface ListShopVouchersByMerchantInputPort {
    List<VoucherResult> execute(String merchantId, int limit);
}
