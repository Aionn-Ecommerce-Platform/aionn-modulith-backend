package com.aionn.promotion.application.port.in.voucher;

import com.aionn.promotion.application.dto.voucher.result.VoucherResult;

import java.util.List;

public interface ListMyShopVouchersInputPort {
    List<VoucherResult> execute(String ownerId, int limit);
}
