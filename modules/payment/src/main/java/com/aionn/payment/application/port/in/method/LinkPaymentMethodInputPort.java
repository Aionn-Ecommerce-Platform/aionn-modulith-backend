package com.aionn.payment.application.port.in.method;

import com.aionn.payment.application.dto.method.command.LinkMethodCommand;
import com.aionn.payment.application.dto.method.result.PaymentMethodResult;

public interface LinkPaymentMethodInputPort {
    PaymentMethodResult execute(LinkMethodCommand command);
}
