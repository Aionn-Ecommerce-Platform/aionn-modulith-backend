package com.aionn.ordering.application.port.in.cart;

import com.aionn.ordering.application.dto.cart.command.UpdateItemQtyCommand;
import com.aionn.ordering.application.dto.cart.result.CartResult;


public interface UpdateItemQtyInputPort {
    CartResult execute(UpdateItemQtyCommand command);
}