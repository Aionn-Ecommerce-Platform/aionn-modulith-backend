package com.aionn.ordering.application.port.in.order;

import com.aionn.ordering.application.dto.order.command.ChangeShippingAddressCommand;
import com.aionn.ordering.application.dto.order.result.OrderResult;

public interface ChangeShippingAddressInputPort {
    OrderResult execute(ChangeShippingAddressCommand command);
}
