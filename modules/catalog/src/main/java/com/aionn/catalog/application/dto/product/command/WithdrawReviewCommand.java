package com.aionn.catalog.application.dto.product.command;

import com.aionn.sharedkernel.application.command.Command;

public record WithdrawReviewCommand(String productId, String merchantId) implements Command {
}
