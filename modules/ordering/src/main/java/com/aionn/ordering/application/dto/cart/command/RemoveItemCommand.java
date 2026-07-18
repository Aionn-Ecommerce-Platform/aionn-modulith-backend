package com.aionn.ordering.application.dto.cart.command;

import com.aionn.sharedkernel.application.command.Command;

public record RemoveItemCommand(String userId, String skuId) implements Command {
}
