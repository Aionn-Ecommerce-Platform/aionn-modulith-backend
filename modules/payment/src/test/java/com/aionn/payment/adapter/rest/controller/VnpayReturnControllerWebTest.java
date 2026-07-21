package com.aionn.payment.adapter.rest.controller;

import com.aionn.payment.adapter.rest.mapper.payment.PaymentDtoMapper;
import com.aionn.payment.adapter.rest.exception.PaymentExceptionHandler;
import com.aionn.payment.application.dto.payment.command.ConfirmPaymentCommand;
import com.aionn.payment.application.dto.payment.result.PaymentResult;
import com.aionn.payment.application.port.in.payment.ConfirmPaymentInputPort;
import com.aionn.payment.application.port.in.payment.FailPaymentInputPort;
import com.aionn.payment.application.port.in.payment.GetPaymentInputPort;
import com.aionn.payment.application.port.out.PaymentProviderClient;
import com.aionn.payment.application.port.out.PaymentProviderRouter;
import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import com.aionn.payment.infrastructure.provider.config.VnpayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class VnpayReturnControllerWebTest {

    @Mock
    private PaymentProviderRouter providerRouter;
    @Mock
    private PaymentProviderClient providerClient;
    @Mock
    private GetPaymentInputPort getPaymentInputPort;
    @Mock
    private ConfirmPaymentInputPort confirmPaymentInputPort;
    @Mock
    private FailPaymentInputPort failPaymentInputPort;
    @Mock
    private VnpayProperties vnpayProperties;
    @Mock
    private PaymentDtoMapper paymentDtoMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(providerRouter.route(PaymentGatewayKind.VNPAY)).thenReturn(providerClient);
        lenient().when(vnpayProperties.frontendReturnUrl()).thenReturn("http://fe/return");

        VnpayReturnController controller = new VnpayReturnController(
                providerRouter, getPaymentInputPort, confirmPaymentInputPort, failPaymentInputPort, vnpayProperties, paymentDtoMapper
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PaymentExceptionHandler())
                .build();
    }

    @Test
    void shouldHandleReturnSuccess() throws Exception {
        PaymentProviderClient.WebhookEvent event = new PaymentProviderClient.WebhookEvent("payment.captured", "pay-1", "txn-1", BigDecimal.TEN, "VND", true, null, null);
        when(providerClient.verifyAndParse(any(), any())).thenReturn(event);

        PaymentResult payment = new PaymentResult("pay-1", "order-1", "user-1", null, BigDecimal.TEN, BigDecimal.ZERO, "VND", "VNPAY", "PAID", "txn-1", null, null, null, Instant.now(), Instant.now(), Instant.now(), null);
        when(getPaymentInputPort.execute("pay-1")).thenReturn(payment);

        mockMvc.perform(get("/api/v1/payments/vnpay/return?vnpay_params=1"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://fe/return?paymentId=pay-1&orderId=order-1"));
    }

    @Test
    void shouldHandleReturnWebhookNoPaymentId() throws Exception {
        PaymentProviderClient.WebhookEvent event = new PaymentProviderClient.WebhookEvent("ping", null, null, null, null, true, null, null);
        when(providerClient.verifyAndParse(any(), any())).thenReturn(event);

        mockMvc.perform(get("/api/v1/payments/vnpay/return?invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldHandleReturnInternalErrorOnFailure() throws Exception {
        PaymentProviderClient.WebhookEvent event = new PaymentProviderClient.WebhookEvent("payment.captured", "pay-1", "txn-1", BigDecimal.TEN, "VND", true, null, null);
        when(providerClient.verifyAndParse(any(), any())).thenReturn(event);
        when(getPaymentInputPort.execute("pay-1")).thenThrow(new com.aionn.payment.domain.exception.PaymentException(com.aionn.payment.domain.exception.PaymentErrorCode.PAYMENT_NOT_FOUND));

        mockMvc.perform(get("/api/v1/payments/vnpay/return?valid"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldHandleIpnSuccess() throws Exception {
        PaymentProviderClient.WebhookEvent event = new PaymentProviderClient.WebhookEvent("payment.captured", "pay-1", "txn-1", BigDecimal.TEN, "VND", true, null, null);
        when(providerClient.verifyAndParse(any(), any())).thenReturn(event);

        PaymentResult payment = new PaymentResult("pay-1", "order-1", "user-1", null, BigDecimal.TEN, BigDecimal.ZERO, "VND", "VNPAY", "INITIATED", null, null, null, null, Instant.now(), Instant.now(), null, null);
        when(getPaymentInputPort.execute("pay-1")).thenReturn(payment);

        ConfirmPaymentCommand command = new ConfirmPaymentCommand("pay-1", "txn-1");
        when(paymentDtoMapper.toConfirmCommand(event)).thenReturn(command);

        mockMvc.perform(post("/api/v1/payments/vnpay/ipn?vnpay_params=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"));

        verify(confirmPaymentInputPort).execute(command);
    }

    @Test
    void shouldHandleIpnInvalidSignature() throws Exception {
        PaymentProviderClient.WebhookEvent event = new PaymentProviderClient.WebhookEvent("ping", null, null, null, null, true, null, null);
        when(providerClient.verifyAndParse(any(), any())).thenReturn(event);

        mockMvc.perform(post("/api/v1/payments/vnpay/ipn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("97"));
    }

    @Test
    void shouldHandleIpnFailureEvent() throws Exception {
        PaymentProviderClient.WebhookEvent event = new PaymentProviderClient.WebhookEvent(
                "payment.failed", "pay-1", "txn-1", BigDecimal.TEN, "VND", false, "VNPAY_24", "Cancelled");
        when(providerClient.verifyAndParse(any(), any())).thenReturn(event);

        PaymentResult payment = new PaymentResult("pay-1", "order-1", "user-1", null, BigDecimal.TEN,
                BigDecimal.ZERO, "VND", "VNPAY", "INITIATED", null, null, null, null,
                Instant.now(), Instant.now(), null, null);
        when(getPaymentInputPort.execute("pay-1")).thenReturn(payment);

        com.aionn.payment.application.dto.payment.command.FailPaymentCommand failCommand =
                new com.aionn.payment.application.dto.payment.command.FailPaymentCommand("pay-1", "VNPAY_ERROR", "Cancelled");
        when(paymentDtoMapper.toFailCommand(event, "VNPAY_ERROR")).thenReturn(failCommand);

        mockMvc.perform(post("/api/v1/payments/vnpay/ipn?vnpay_params=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"));

        verify(failPaymentInputPort).execute(failCommand);
    }

    @Test
    void shouldHandleIpnAlreadyPaid() throws Exception {
        PaymentProviderClient.WebhookEvent event = new PaymentProviderClient.WebhookEvent(
                "payment.captured", "pay-1", "txn-1", BigDecimal.TEN, "VND", true, null, null);
        when(providerClient.verifyAndParse(any(), any())).thenReturn(event);

        PaymentResult payment = new PaymentResult("pay-1", "order-1", "user-1", null, BigDecimal.TEN,
                BigDecimal.ZERO, "VND", "VNPAY", "PAID", "txn-1", null, null, null,
                Instant.now(), Instant.now(), Instant.now(), null);
        when(getPaymentInputPort.execute("pay-1")).thenReturn(payment);

        mockMvc.perform(post("/api/v1/payments/vnpay/ipn?vnpay_params=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"));

        verifyNoInteractions(confirmPaymentInputPort, failPaymentInputPort);
    }

    @Test
    void shouldHandleIpnPaymentNotFound() throws Exception {
        PaymentProviderClient.WebhookEvent event = new PaymentProviderClient.WebhookEvent(
                "payment.captured", "pay-missing", "txn-1", BigDecimal.TEN, "VND", true, null, null);
        when(providerClient.verifyAndParse(any(), any())).thenReturn(event);
        when(getPaymentInputPort.execute("pay-missing")).thenThrow(
                new com.aionn.payment.domain.exception.PaymentException(
                        com.aionn.payment.domain.exception.PaymentErrorCode.PAYMENT_NOT_FOUND));

        mockMvc.perform(post("/api/v1/payments/vnpay/ipn?vnpay_params=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("01"));
    }

    @Test
    void shouldHandleIpnUnknownPaymentException() throws Exception {
        PaymentProviderClient.WebhookEvent event = new PaymentProviderClient.WebhookEvent(
                "payment.captured", "pay-1", "txn-1", BigDecimal.TEN, "VND", true, null, null);
        when(providerClient.verifyAndParse(any(), any())).thenReturn(event);
        when(getPaymentInputPort.execute("pay-1")).thenThrow(
                new com.aionn.payment.domain.exception.PaymentException(
                        com.aionn.payment.domain.exception.PaymentErrorCode.PAYMENT_GATEWAY_ERROR));

        mockMvc.perform(post("/api/v1/payments/vnpay/ipn?vnpay_params=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("99"));
    }
}
