package com.aionn.catalog.application.dto.merchant.command;

import com.aionn.sharedkernel.application.command.Command;

public record RegisterMerchantCommand(String ownerId, String name) implements Command {
}
