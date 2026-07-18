package com.aionn.ordering.application.port.in.cart;

import com.aionn.ordering.application.dto.cart.command.ApplyVoucherCommand;
import com.aionn.ordering.application.dto.cart.result.CartResult;


public interface ApplyVoucherInputPort {
    CartResult execute(ApplyVoucherCommand command);
}