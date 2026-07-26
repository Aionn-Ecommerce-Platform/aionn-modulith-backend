package com.aionn.promotion.application.port.in.voucher;

import com.aionn.promotion.application.dto.voucher.result.UserVoucherResult;

public interface GetMyVoucherInputPort {
    UserVoucherResult execute(String userId, String voucherCode);
}
