package com.aionn.ordering.application.port.in.order;

import com.aionn.ordering.application.dto.order.command.ConfirmShippedCommand;
import com.aionn.ordering.application.dto.order.result.OrderResult;


public interface ConfirmShippedInputPort {
    OrderResult execute(ConfirmShippedCommand command);
}