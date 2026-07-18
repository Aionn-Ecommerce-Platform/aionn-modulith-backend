package com.aionn.ordering.application.port.in.cart;

import com.aionn.ordering.application.dto.cart.command.RemoveItemCommand;
import com.aionn.ordering.application.dto.cart.result.CartResult;


public interface RemoveItemInputPort {
    CartResult execute(RemoveItemCommand command);
}