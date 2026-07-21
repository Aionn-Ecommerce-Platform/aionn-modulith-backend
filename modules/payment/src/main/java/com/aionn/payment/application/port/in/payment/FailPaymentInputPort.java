package com.aionn.payment.application.port.in.payment;

import com.aionn.payment.application.dto.payment.command.FailPaymentCommand;
import com.aionn.payment.application.dto.payment.result.PaymentResult;

public interface FailPaymentInputPort {
    PaymentResult execute(FailPaymentCommand command);
}
