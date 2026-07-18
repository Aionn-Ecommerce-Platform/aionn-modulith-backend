package com.aionn.ordering.application.dto.cart.command;

import com.aionn.sharedkernel.application.command.Command;

public record UpdateItemQtyCommand(String userId, String skuId, int newQty) implements Command {
}
