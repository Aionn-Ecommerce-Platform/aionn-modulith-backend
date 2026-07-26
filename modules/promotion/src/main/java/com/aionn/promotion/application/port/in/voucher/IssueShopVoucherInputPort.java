package com.aionn.promotion.application.port.in.voucher;

import com.aionn.promotion.application.dto.voucher.command.VoucherCommands;
import com.aionn.promotion.application.dto.voucher.result.VoucherResult;

public interface IssueShopVoucherInputPort {
    VoucherResult execute(VoucherCommands.IssueShopVoucher command);
}
