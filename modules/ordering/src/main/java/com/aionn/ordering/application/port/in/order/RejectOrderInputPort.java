package com.aionn.ordering.application.port.in.order;

import com.aionn.ordering.application.dto.order.command.RejectOrderCommand;
import com.aionn.ordering.application.dto.order.result.OrderResult;


public interface RejectOrderInputPort {
    OrderResult execute(RejectOrderCommand command);
}