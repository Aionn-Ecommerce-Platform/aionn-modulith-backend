package com.aionn.ordering.infrastructure.integration.payment;

import com.aionn.ordering.application.port.out.PaymentGateway;
import com.aionn.sharedkernel.integration.port.payment.PaymentInitiatePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentInitiateAdapterTest {

    @Mock
    private PaymentInitiatePort paymentInitiatePort;

    @InjectMocks
    private PaymentInitiateAdapter adapter;

    @Test
    void authorizeReturnsCapturedAuthorization() {
        PaymentInitiatePort.InitResult initResult =
                new PaymentInitiatePort.InitResult("pay-100", null, true);
        when(paymentInitiatePort.initPayment(eq("ord-1"), eq("usr-1"), eq("pm-1"),
                eq(BigDecimal.TEN), eq("VND"), eq("vnpay"), anyString()))
                .thenReturn(initResult);

        PaymentGateway.PaymentAuthorization auth =
                adapter.authorize("ord-1", "usr-1", "pm-1", BigDecimal.TEN, "VND", "vnpay");

        assertEquals("pay-100", auth.paymentId());
        assertTrue(auth.approved());
    }

    @Test
    void authorizeReturnsRedirectRequiredWhenNotCapturedWithUrl() {
        PaymentInitiatePort.InitResult initResult =
                new PaymentInitiatePort.InitResult("pay-101", "http://vnpay.vn/pay", false);
        when(paymentInitiatePort.initPayment(eq("ord-1"), eq("usr-1"), eq("pm-1"),
                eq(BigDecimal.TEN), eq("VND"), eq("vnpay"), anyString()))
                .thenReturn(initResult);

        PaymentGateway.PaymentAuthorization auth =
                adapter.authorize("ord-1", "usr-1", "pm-1", BigDecimal.TEN, "VND", "vnpay");

        assertEquals("pay-101", auth.paymentId());
        assertFalse(auth.approved());
        assertEquals("async-redirect-required", auth.declineReason());
    }

    @Test
    void refundDelegatesToInitiatePort() {
        adapter.refund("pay-100", BigDecimal.TEN, "VND", "order return");

        verify(paymentInitiatePort).refund(eq("pay-100"), eq(BigDecimal.TEN), eq("VND"), eq("order return"), anyString());
    }
}
