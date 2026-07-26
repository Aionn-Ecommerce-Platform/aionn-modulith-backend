package com.aionn.promotion.application.port.in.voucher;

import com.aionn.promotion.application.dto.voucher.result.UserVoucherResult;

import java.util.List;

public interface ListMyVouchersInputPort {
    List<UserVoucherResult> execute(String userId, int limit);
}
