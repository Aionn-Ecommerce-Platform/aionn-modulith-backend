package com.aionn.payment.application.service;

import com.aionn.payment.application.dto.payment.command.ConfirmPaymentCommand;
import com.aionn.payment.application.dto.payment.command.FailPaymentCommand;
import com.aionn.payment.application.dto.payment.result.PaymentResult;
import com.aionn.payment.application.port.in.payment.ConfirmPaymentInputPort;
import com.aionn.payment.application.port.in.payment.FailPaymentInputPort;
import com.aionn.payment.application.port.in.payment.GetPaymentInputPort;
import com.aionn.payment.application.port.out.PaymentProviderClient;
import com.aionn.payment.application.port.out.PaymentProviderRouter;
import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import com.aionn.payment.domain.valueobject.VnpayResponseCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VnpayWebhookServiceTest {

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

    @InjectMocks
    private VnpayWebhookService service;

    @Test
    void handleReturnConfirmsAndResolvesPayment() {
        PaymentProviderClient.WebhookEvent event = new PaymentProviderClient.WebhookEvent(
                "payment.captured", "pay-1", "txn-1", new BigDecimal("100"), "VND", true, null, null);
        PaymentResult payment = mock(PaymentResult.class);
        when(payment.paymentId()).thenReturn("pay-1");
        when(payment.orderId()).thenReturn("order-1");
        when(providerRouter.route(PaymentGatewayKind.VNPAY)).thenReturn(providerClient);
        when(providerClient.verifyAndParse(any(), any())).thenReturn(event);
        when(getPaymentInputPort.execute("pay-1")).thenReturn(payment);

        VnpayWebhookService.ReturnResult result = service.handleReturn("query");

        assertEquals(VnpayWebhookService.ReturnStatus.SUCCESS, result.status());
        assertEquals("pay-1", result.paymentId());
        assertEquals("order-1", result.orderId());
        verify(confirmPaymentInputPort).execute(any(ConfirmPaymentCommand.class));
    }

    @Test
    void handleReturnRejectsMissingPaymentId() {
        PaymentProviderClient.WebhookEvent event = new PaymentProviderClient.WebhookEvent(
                "vnpay.ping", null, null, null, null, true, null, null);
        when(providerRouter.route(PaymentGatewayKind.VNPAY)).thenReturn(providerClient);
        when(providerClient.verifyAndParse(any(), any())).thenReturn(event);

        VnpayWebhookService.ReturnResult result = service.handleReturn("query");

        assertEquals(VnpayWebhookService.ReturnStatus.BAD_REQUEST, result.status());
        verifyNoInteractions(confirmPaymentInputPort, failPaymentInputPort);
    }

    @Test
    void handleReturnReportsInternalErrorWhenFinalisationFails() {
        PaymentProviderClient.WebhookEvent event = new PaymentProviderClient.WebhookEvent(
                "payment.captured", "pay-1", "txn-1", new BigDecimal("100"), "VND", true, null, null);
        when(providerRouter.route(PaymentGatewayKind.VNPAY)).thenReturn(providerClient);
        when(providerClient.verifyAndParse(any(), any())).thenReturn(event);
        when(confirmPaymentInputPort.execute(any())).thenThrow(new IllegalStateException("db down"));

        VnpayWebhookService.ReturnResult result = service.handleReturn("query");

        assertEquals(VnpayWebhookService.ReturnStatus.INTERNAL_ERROR, result.status());
    }

    @Test
    void handleIpnConfirmsSuccessfulEvent() {
        PaymentProviderClient.WebhookEvent event = new PaymentProviderClient.WebhookEvent(
                "payment.captured", "pay-1", "txn-1", new BigDecimal("100"), "VND", true, null, null);
        when(providerRouter.route(PaymentGatewayKind.VNPAY)).thenReturn(providerClient);
        when(providerClient.verifyAndParse(any(), any())).thenReturn(event);

        VnpayWebhookService.IpnResult result = service.handleIpn("query");

        assertEquals(VnpayResponseCode.CONFIRM_SUCCESS, result.code());
        verify(confirmPaymentInputPort).execute(any(ConfirmPaymentCommand.class));
    }

    @Test
    void handleIpnReturnsInvalidSignatureCode() {
        PaymentProviderClient.WebhookEvent event = new PaymentProviderClient.WebhookEvent(
                "vnpay.ping", null, null, null, null, true, null, null);
        when(providerRouter.route(PaymentGatewayKind.VNPAY)).thenReturn(providerClient);
        when(providerClient.verifyAndParse(any(), any())).thenReturn(event);

        VnpayWebhookService.IpnResult result = service.handleIpn("query");

        assertEquals(VnpayResponseCode.INVALID_SIGNATURE, result.code());
    }

    @Test
    void handleIpnMapsPaymentNotFoundCode() {
        PaymentProviderClient.WebhookEvent event = new PaymentProviderClient.WebhookEvent(
                "payment.captured", "pay-missing", "txn-1", new BigDecimal("100"), "VND", true, null, null);
        when(providerRouter.route(PaymentGatewayKind.VNPAY)).thenReturn(providerClient);
        when(providerClient.verifyAndParse(any(), any())).thenReturn(event);
        when(confirmPaymentInputPort.execute(any()))
                .thenThrow(new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        VnpayWebhookService.IpnResult result = service.handleIpn("query");

        assertEquals(VnpayResponseCode.ORDER_NOT_FOUND, result.code());
    }

    @Test
    void handleIpnMapsOtherPaymentErrorsToUnknownCode() {
        PaymentProviderClient.WebhookEvent event = new PaymentProviderClient.WebhookEvent(
                "payment.failed", "pay-1", "txn-1", new BigDecimal("100"), "VND", false, "VNPAY_24", "Cancelled");
        when(providerRouter.route(PaymentGatewayKind.VNPAY)).thenReturn(providerClient);
        when(providerClient.verifyAndParse(any(), any())).thenReturn(event);
        when(failPaymentInputPort.execute(any()))
                .thenThrow(new PaymentException(PaymentErrorCode.PAYMENT_GATEWAY_ERROR));

        VnpayWebhookService.IpnResult result = service.handleIpn("query");

        assertEquals(VnpayResponseCode.UNKNOWN_ERROR, result.code());
        ArgumentCaptor<FailPaymentCommand> captor = ArgumentCaptor.forClass(FailPaymentCommand.class);
        verify(failPaymentInputPort).execute(captor.capture());
        assertEquals("VNPAY_24", captor.getValue().errorCode());
        verify(confirmPaymentInputPort, never()).execute(any());
    }
}
