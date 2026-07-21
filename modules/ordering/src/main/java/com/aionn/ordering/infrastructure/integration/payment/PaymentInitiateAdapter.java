package com.aionn.ordering.infrastructure.integration.payment;

import com.aionn.ordering.application.port.out.PaymentGateway;
import com.aionn.sharedkernel.integration.port.payment.PaymentInitiatePort;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component("orderingPaymentInitiateAdapter")
@RequiredArgsConstructor
public class PaymentInitiateAdapter implements PaymentGateway {

    private final PaymentInitiatePort paymentInitiatePort;

    @Override
    public PaymentAuthorization authorize(String orderId, String userId, String paymentMethodId,
            BigDecimal amount, String currency, String gateway) {
        PaymentInitiatePort.InitResult result = paymentInitiatePort.initPayment(
                orderId, userId, paymentMethodId, amount, currency,
                gateway, IdGenerator.ulid());
        if (result.captured()) {
            return new PaymentAuthorization(result.paymentId(), true, null);
        }
        return new PaymentAuthorization(result.paymentId(), false,
                result.redirectUrl() != null ? "async-redirect-required" : "not-captured");
    }

    @Override
    public void refund(String paymentId, BigDecimal amount, String currency, String reason) {
        paymentInitiatePort.refund(paymentId, amount, currency, reason, IdGenerator.ulid());
    }
}
