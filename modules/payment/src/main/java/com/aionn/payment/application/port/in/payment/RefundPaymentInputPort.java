package com.aionn.payment.application.port.in.payment;

import com.aionn.payment.application.dto.payment.command.RefundPaymentCommand;
import com.aionn.payment.application.dto.payment.result.PaymentResult;

public interface RefundPaymentInputPort {
    PaymentResult execute(RefundPaymentCommand command);
}
