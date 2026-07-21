package com.aionn.payment.infrastructure.provider;

import com.aionn.payment.application.port.out.PaymentProviderClient;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import com.aionn.payment.infrastructure.provider.config.StripeProperties;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.HasId;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class StripePaymentProviderClientTest {

    @Mock
    private StripeProperties properties;
    @Mock
    private MerchantQueryPort merchantQueryPort;

    private StripePaymentProviderClient client;

    @BeforeEach
    void setUp() {
        lenient().when(properties.apiKey()).thenReturn("sk_test_123");
        client = new StripePaymentProviderClient(properties, merchantQueryPort);
    }

    @Test
    void kindShouldReturnStripe() {
        assertEquals(PaymentGatewayKind.STRIPE, client.kind());
    }

    @Test
    void configureStripeApiKeyShouldSetKeyWithoutThrowing() {
        assertDoesNotThrow(() -> StripePaymentProviderClient.configureStripeApiKey("sk_test_123"));
    }

    @Test
    void configureStripeApiKeyShouldIgnoreNullKey() {
        assertDoesNotThrow(() -> StripePaymentProviderClient.configureStripeApiKey(null));
    }

    @Test
    void configureStripeApiKeyShouldIgnoreBlankKey() {
        assertDoesNotThrow(() -> StripePaymentProviderClient.configureStripeApiKey("  "));
    }

    @Test
    void generateInvoiceShouldReturnFormattedUrl() {
        String invoice = client.generateInvoice("pay-123", "order-456", BigDecimal.TEN, "USD");
        assertNotNull(invoice);
        assertTrue(invoice.contains("pay-123"));
    }

    @Test
    void verifyAndParseShouldRejectMissingSignature() {
        PaymentProviderClient.WebhookEvent event = client.verifyAndParse("{}", null);
        assertNotNull(event);
        assertEquals("MISSING_SIGNATURE", event.errorCode());
    }

    @Test
    void verifyAndParseShouldRejectBlankSignature() {
        PaymentProviderClient.WebhookEvent event = client.verifyAndParse("{}", "  ");
        assertNotNull(event);
        assertEquals("MISSING_SIGNATURE", event.errorCode());
    }

    @Test
    void verifyAndParseShouldReturnParseFailedForBadPayload() {
        PaymentProviderClient.WebhookEvent event = client.verifyAndParse("not-json", "sig_bad");
        assertNotNull(event);
        assertFalse(event.success());
    }

    @Test
    void refundWithoutTransactionNoShouldFail() {
        PaymentProviderClient.Refund refund = client.refund(
                new PaymentProviderClient.RefundRequest("p-1", null, BigDecimal.TEN, "USD", "reason"));
        assertNotNull(refund);
        assertFalse(refund.accepted());
    }

    @Test
    void refundWithBlankTransactionNoShouldFail() {
        PaymentProviderClient.Refund refund = client.refund(
                new PaymentProviderClient.RefundRequest("p-1", "  ", BigDecimal.TEN, "USD", "reason"));
        assertNotNull(refund);
        assertFalse(refund.accepted());
    }

    @Test
    void refundWithValidTransactionNoShouldAttemptStripeCall() {
        try (MockedStatic<Refund> mockedRefund = Mockito.mockStatic(Refund.class)) {
            Refund mockStripeRefund = mock(Refund.class);
            when(mockStripeRefund.getId()).thenReturn("ref_abc");
            when(mockStripeRefund.getStatus()).thenReturn("succeeded");

            mockedRefund.when(() -> Refund.create(any(com.stripe.param.RefundCreateParams.class), any(RequestOptions.class))).thenReturn(mockStripeRefund);
            mockedRefund.when(() -> Refund.create(any(com.stripe.param.RefundCreateParams.class))).thenReturn(mockStripeRefund);

            PaymentProviderClient.Refund refund = client.refund(
                    new PaymentProviderClient.RefundRequest("p-1", "pi_test_abc", BigDecimal.TEN, "USD", "reason"));
            assertNotNull(refund);
            assertTrue(refund.accepted());
            assertEquals("ref_abc", refund.refundTransactionNo());
        }
    }

    @Test
    void refundWithVndCurrencyZeroDecimalShouldAttemptStripeCall() {
        try (MockedStatic<Refund> mockedRefund = Mockito.mockStatic(Refund.class)) {
            Refund mockStripeRefund = mock(Refund.class);
            when(mockStripeRefund.getId()).thenReturn("ref_abc");
            when(mockStripeRefund.getStatus()).thenReturn("pending");

            mockedRefund.when(() -> Refund.create(any(com.stripe.param.RefundCreateParams.class), any(RequestOptions.class))).thenReturn(mockStripeRefund);
            mockedRefund.when(() -> Refund.create(any(com.stripe.param.RefundCreateParams.class))).thenReturn(mockStripeRefund);

            PaymentProviderClient.Refund refund = client.refund(
                    new PaymentProviderClient.RefundRequest("p-1", "pi_test_abc",
                            BigDecimal.valueOf(100000), "VND", null));
            assertNotNull(refund);
            assertTrue(refund.accepted());
        }
    }

    @Test
    void authorizeShouldHandleSucceededStatus() {
        try (MockedStatic<PaymentIntent> mockedIntent = Mockito.mockStatic(PaymentIntent.class)) {
            PaymentIntent mockIntent = mock(PaymentIntent.class);
            when(mockIntent.getId()).thenReturn("pi_123");
            when(mockIntent.getStatus()).thenReturn("succeeded");

            mockedIntent.when(() -> PaymentIntent.create(any(com.stripe.param.PaymentIntentCreateParams.class), any(RequestOptions.class))).thenReturn(mockIntent);
            mockedIntent.when(() -> PaymentIntent.create(any(com.stripe.param.PaymentIntentCreateParams.class))).thenReturn(mockIntent);

            PaymentProviderClient.AuthorizationRequest request = new PaymentProviderClient.AuthorizationRequest(
                    "pay-1", "order-1", "user-1", "merchant-1", "tok_visa", BigDecimal.TEN, "USD", "idemp-key", "url"
            );

            PaymentProviderClient.Authorization auth = client.authorize(request);
            assertNotNull(auth);
            assertTrue(auth.captured());
            assertEquals("pi_123", auth.transactionNo());
        }
    }

    @Test
    void authorizeShouldHandleRequiresActionStatus() {
        try (MockedStatic<PaymentIntent> mockedIntent = Mockito.mockStatic(PaymentIntent.class)) {
            PaymentIntent mockIntent = mock(PaymentIntent.class);
            when(mockIntent.getId()).thenReturn("pi_123");
            when(mockIntent.getStatus()).thenReturn("requires_action");

            PaymentIntent.NextAction mockAction = mock(PaymentIntent.NextAction.class);
            com.stripe.model.PaymentIntent.NextAction.RedirectToUrl mockRedirect = mock(com.stripe.model.PaymentIntent.NextAction.RedirectToUrl.class);
            when(mockRedirect.getUrl()).thenReturn("http://3dsecure-url");
            when(mockAction.getRedirectToUrl()).thenReturn(mockRedirect);
            when(mockIntent.getNextAction()).thenReturn(mockAction);

            mockedIntent.when(() -> PaymentIntent.create(any(com.stripe.param.PaymentIntentCreateParams.class), any(RequestOptions.class))).thenReturn(mockIntent);
            mockedIntent.when(() -> PaymentIntent.create(any(com.stripe.param.PaymentIntentCreateParams.class))).thenReturn(mockIntent);

            PaymentProviderClient.AuthorizationRequest request = new PaymentProviderClient.AuthorizationRequest(
                    "pay-1", "order-1", "user-1", null, null, BigDecimal.TEN, "USD", "idemp-key", "url"
            );

            PaymentProviderClient.Authorization auth = client.authorize(request);
            assertNotNull(auth);
            assertFalse(auth.captured());
            assertEquals("http://3dsecure-url", auth.authUrl());
        }
    }

    @Test
    void webhookVerifyAndParseValidSucceededIntent() {
        when(properties.webhookSecret()).thenReturn("whsec_secret");

        Event mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn("payment_intent.succeeded");

        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getId()).thenReturn("pi_123");
        when(mockIntent.getAmount()).thenReturn(1000L);
        when(mockIntent.getCurrency()).thenReturn("usd");
        when(mockIntent.getMetadata()).thenReturn(Map.of("paymentId", "pay-abc"));

        StripeObject mockHasId = mockIntent;

        com.stripe.model.EventDataObjectDeserializer mockDeserializer = mock(com.stripe.model.EventDataObjectDeserializer.class);
        when(mockDeserializer.getObject()).thenReturn(Optional.of(mockHasId));
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);

        try (MockedStatic<Webhook> mockedWebhook = Mockito.mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(mockEvent);

            PaymentProviderClient.WebhookEvent result = client.verifyAndParse("payload", "sig");
            assertNotNull(result);
            assertTrue(result.success());
            assertEquals("pay-abc", result.paymentId());
            assertEquals("pi_123", result.transactionNo());
            assertEquals(BigDecimal.TEN.setScale(2), result.amount());
        }
    }
}
