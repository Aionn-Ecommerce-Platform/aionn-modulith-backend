package com.aionn.promotion.application.port.in.voucher;

import com.aionn.promotion.application.dto.voucher.command.VoucherCommands;
import com.aionn.promotion.application.dto.voucher.result.UserVoucherResult;

public interface ReserveVoucherInputPort {
    UserVoucherResult execute(VoucherCommands.ReserveVoucher command);
}
