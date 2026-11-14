package com.aionn.payment.application.service;

import com.aionn.payment.application.dto.payment.command.ConfirmPaymentCommand;
import com.aionn.payment.application.dto.payment.command.FailPaymentCommand;
import com.aionn.payment.application.port.in.payment.ConfirmPaymentInputPort;
import com.aionn.payment.application.port.in.payment.FailPaymentInputPort;
import com.aionn.payment.application.port.out.PaymentProviderClient;
import com.aionn.payment.application.port.out.PaymentProviderRouter;
import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookServiceTest {

    @Mock
    private PaymentProviderRouter providerRouter;
    @Mock
    private PaymentProviderClient providerClient;
    @Mock
    private ConfirmPaymentInputPort confirmPaymentInputPort;
    @Mock
    private FailPaymentInputPort failPaymentInputPort;

    @InjectMocks
    private PaymentWebhookService service;

    @Test
    void rejectsUnknownGateway() {
        assertFalse(service.handle("unknown", new byte[0], null));
        verifyNoInteractions(providerRouter, confirmPaymentInputPort, failPaymentInputPort);
    }

    @Test
    void confirmsSuccessfulPayment() {
        PaymentProviderClient.WebhookEvent event = new PaymentProviderClient.WebhookEvent(
                "payment.captured", "pay-1", "txn-1", new BigDecimal("100"), "VND", true, null, null);
        when(providerRouter.route(PaymentGatewayKind.STRIPE)).thenReturn(providerClient);
        when(providerClient.verifyAndParse(any(), any())).thenReturn(event);

        assertTrue(service.handle("stripe", "{}".getBytes(StandardCharsets.UTF_8), "sig"));

        ArgumentCaptor<ConfirmPaymentCommand> captor = ArgumentCaptor.forClass(ConfirmPaymentCommand.class);
        verify(confirmPaymentInputPort).execute(captor.capture());
        assertEquals("pay-1", captor.getValue().paymentId());
        assertEquals("txn-1", captor.getValue().transactionNo());
        verify(failPaymentInputPort, never()).execute(any());
    }

    @Test
    void failsFailedPaymentWithGatewayDefaultCode() {
        PaymentProviderClient.WebhookEvent event = new PaymentProviderClient.WebhookEvent(
                "payment.failed", "pay-1", "txn-1", new BigDecimal("100"), "VND", false, null, "declined");
        when(providerRouter.route(PaymentGatewayKind.STRIPE)).thenReturn(providerClient);
        when(providerClient.verifyAndParse(any(), any())).thenReturn(event);

        assertTrue(service.handle("stripe", "{}".getBytes(StandardCharsets.UTF_8), "sig"));

        ArgumentCaptor<FailPaymentCommand> captor = ArgumentCaptor.forClass(FailPaymentCommand.class);
        verify(failPaymentInputPort).execute(captor.capture());
        assertEquals("GATEWAY_ERROR", captor.getValue().errorCode());
        verify(confirmPaymentInputPort, never()).execute(any());
    }

    @Test
    void acknowledgesIgnorableStripeEventWithoutPaymentId() {
        PaymentProviderClient.WebhookEvent event = new PaymentProviderClient.WebhookEvent(
                "stripe.customer.created", null, null, null, null, true, null, null);
        when(providerRouter.route(PaymentGatewayKind.STRIPE)).thenReturn(providerClient);
        when(providerClient.verifyAndParse(any(), any())).thenReturn(event);

        assertTrue(service.handle("stripe", new byte[0], "sig"));
        verifyNoInteractions(confirmPaymentInputPort, failPaymentInputPort);
    }

    @Test
    void rejectsEventWithoutPaymentIdForNonStripeGateway() {
        PaymentProviderClient.WebhookEvent event = new PaymentProviderClient.WebhookEvent(
                "vnpay.ping", null, null, null, null, true, null, null);
        when(providerRouter.route(PaymentGatewayKind.VNPAY)).thenReturn(providerClient);
        when(providerClient.verifyAndParse(any(), any())).thenReturn(event);

        assertFalse(service.handle("vnpay", new byte[0], "sig"));
        verifyNoInteractions(confirmPaymentInputPort, failPaymentInputPort);
    }
}
