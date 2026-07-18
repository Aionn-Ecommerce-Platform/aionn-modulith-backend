package com.aionn.ordering.application.port.in.order;

import com.aionn.ordering.application.dto.order.command.PlaceOrderCommand;
import com.aionn.ordering.application.dto.order.result.OrderResult;


public interface PlaceOrderInputPort {
    OrderResult execute(PlaceOrderCommand command);
}