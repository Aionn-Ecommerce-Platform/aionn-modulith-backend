package com.aionn.promotion.application.port.in.voucher;

import com.aionn.promotion.application.dto.voucher.result.VoucherSummaryResult;

public interface GetVoucherSummaryInputPort {
    VoucherSummaryResult execute();
}
