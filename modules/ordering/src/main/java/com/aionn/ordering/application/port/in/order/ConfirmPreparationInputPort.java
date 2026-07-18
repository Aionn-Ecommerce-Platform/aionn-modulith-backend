package com.aionn.ordering.application.port.in.order;

import com.aionn.ordering.application.dto.order.command.ConfirmPreparationCommand;
import com.aionn.ordering.application.dto.order.result.OrderResult;


public interface ConfirmPreparationInputPort {
    OrderResult execute(ConfirmPreparationCommand command);
}