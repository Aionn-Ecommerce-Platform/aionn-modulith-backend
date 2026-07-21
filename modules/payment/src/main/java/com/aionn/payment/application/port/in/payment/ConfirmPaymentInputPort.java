package com.aionn.payment.application.port.in.payment;

import com.aionn.payment.application.dto.payment.command.ConfirmPaymentCommand;
import com.aionn.payment.application.dto.payment.result.PaymentResult;

public interface ConfirmPaymentInputPort {
    PaymentResult execute(ConfirmPaymentCommand command);
}
