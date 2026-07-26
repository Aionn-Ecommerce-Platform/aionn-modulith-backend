package com.aionn.promotion.application.dto.voucher.command;

import com.aionn.sharedkernel.application.command.Command;

public record ReleaseVoucherCommand(
        String userId,
        String voucherCode,
        String orderId,
        String reason) implements Command {
}
