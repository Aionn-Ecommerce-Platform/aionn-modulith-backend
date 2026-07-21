package com.aionn.payment.application.port.in.method;

import com.aionn.payment.application.dto.method.result.PaymentMethodResult;

import java.util.List;

public interface ListPaymentMethodsInputPort {
    List<PaymentMethodResult> execute(String userId);
}
