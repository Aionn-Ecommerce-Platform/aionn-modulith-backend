package com.aionn.catalog.application.dto.merchant.command;

import com.aionn.sharedkernel.application.command.Command;

public record CloseMerchantCommand(String merchantId, String ownerId, String reason) implements Command {
}
