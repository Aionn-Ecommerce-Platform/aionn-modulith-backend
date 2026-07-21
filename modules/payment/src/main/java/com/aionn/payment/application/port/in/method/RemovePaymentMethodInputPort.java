package com.aionn.payment.application.port.in.method;

import com.aionn.payment.application.dto.method.command.RemoveMethodCommand;

public interface RemovePaymentMethodInputPort {
    void execute(RemoveMethodCommand command);
}
