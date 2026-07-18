package com.aionn.ordering.application.dto.cart.command;

import com.aionn.sharedkernel.application.command.Command;

public record AddItemCommand(String userId, String skuId, int qty) implements Command {
}
