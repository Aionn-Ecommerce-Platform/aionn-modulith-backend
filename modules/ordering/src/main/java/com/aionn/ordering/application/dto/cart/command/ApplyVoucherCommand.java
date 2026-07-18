package com.aionn.ordering.application.dto.cart.command;

import com.aionn.sharedkernel.application.command.Command;

public record ApplyVoucherCommand(String userId, String voucherCode) implements Command {
}
