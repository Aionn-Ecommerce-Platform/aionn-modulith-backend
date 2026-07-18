package com.aionn.ordering.application.port.in.order;

import com.aionn.ordering.application.dto.order.command.CancelOrderCommand;
import com.aionn.ordering.application.dto.order.result.OrderResult;


public interface CancelOrderInputPort {
    OrderResult execute(CancelOrderCommand command);
}