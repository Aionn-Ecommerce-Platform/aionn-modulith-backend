package com.aionn.payment.application.port.in.payment;

import com.aionn.payment.application.dto.payment.command.InitiatePaymentCommand;
import com.aionn.payment.application.dto.payment.result.PaymentResult;

public interface InitiatePaymentInputPort {
    PaymentResult execute(InitiatePaymentCommand command);
}
