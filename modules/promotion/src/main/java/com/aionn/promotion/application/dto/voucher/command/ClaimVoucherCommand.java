package com.aionn.promotion.application.dto.voucher.command;

import com.aionn.sharedkernel.application.command.Command;

public record ClaimVoucherCommand(String userId, String voucherCode) implements Command {
}
