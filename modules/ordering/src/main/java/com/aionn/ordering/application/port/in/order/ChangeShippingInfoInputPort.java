package com.aionn.ordering.application.port.in.order;

import com.aionn.ordering.application.dto.order.command.ChangeShippingInfoCommand;
import com.aionn.ordering.application.dto.order.result.OrderResult;


public interface ChangeShippingInfoInputPort {
    OrderResult execute(ChangeShippingInfoCommand command);
}