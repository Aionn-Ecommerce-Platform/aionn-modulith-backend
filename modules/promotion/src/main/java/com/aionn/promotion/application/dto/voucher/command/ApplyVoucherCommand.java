package com.aionn.promotion.application.dto.voucher.command;

import com.aionn.sharedkernel.application.command.Command;

import java.math.BigDecimal;

public record ApplyVoucherCommand(
        String userId,
        String voucherCode,
        String orderId,
        BigDecimal appliedAmount,
        String currency) implements Command {
}
