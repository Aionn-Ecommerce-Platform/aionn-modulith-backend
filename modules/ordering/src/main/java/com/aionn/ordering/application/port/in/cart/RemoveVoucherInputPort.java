package com.aionn.ordering.application.port.in.cart;

import com.aionn.ordering.application.dto.cart.command.RemoveVoucherCommand;
import com.aionn.ordering.application.dto.cart.result.CartResult;


public interface RemoveVoucherInputPort {
    CartResult execute(RemoveVoucherCommand command);
}