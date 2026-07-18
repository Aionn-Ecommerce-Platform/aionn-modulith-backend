package com.aionn.ordering.application.port.in.cart;

import com.aionn.ordering.application.dto.cart.command.ClearCartCommand;
import com.aionn.ordering.application.dto.cart.result.CartResult;


public interface ClearCartInputPort {
    CartResult execute(ClearCartCommand command);
}