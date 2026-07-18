package com.aionn.ordering.application.port.in.cart;

import com.aionn.ordering.application.dto.cart.command.AddItemCommand;
import com.aionn.ordering.application.dto.cart.result.CartResult;


public interface AddItemInputPort {
    CartResult execute(AddItemCommand command);
}