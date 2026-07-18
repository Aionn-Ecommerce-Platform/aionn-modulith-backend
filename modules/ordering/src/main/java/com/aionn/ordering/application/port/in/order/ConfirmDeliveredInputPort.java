package com.aionn.ordering.application.port.in.order;

import com.aionn.ordering.application.dto.order.command.ConfirmDeliveredCommand;
import com.aionn.ordering.application.dto.order.result.OrderResult;


public interface ConfirmDeliveredInputPort {
    OrderResult execute(ConfirmDeliveredCommand command);
}