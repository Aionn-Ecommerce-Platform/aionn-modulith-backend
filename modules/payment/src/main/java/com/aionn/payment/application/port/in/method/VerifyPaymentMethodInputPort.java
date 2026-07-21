package com.aionn.payment.application.port.in.method;

import com.aionn.payment.application.dto.method.command.VerifyMethodCommand;
import com.aionn.payment.application.dto.method.result.PaymentMethodResult;

public interface VerifyPaymentMethodInputPort {
    PaymentMethodResult execute(VerifyMethodCommand command);
}
