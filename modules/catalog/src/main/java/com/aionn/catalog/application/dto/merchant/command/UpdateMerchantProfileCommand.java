package com.aionn.catalog.application.dto.merchant.command;

import com.aionn.sharedkernel.application.command.Command;

public record UpdateMerchantProfileCommand(
                String merchantId,
                String ownerId,
                String name,
                String logoUrl,
                String description,
                String provinceCode) implements Command {
}
