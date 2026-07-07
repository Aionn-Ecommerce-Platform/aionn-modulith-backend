package com.aionn.catalog.application.dto.merchant.command;

import com.aionn.sharedkernel.application.command.Command;

public record SuspendMerchantCommand(String merchantId, String adminId, String reason) implements Command {
}
