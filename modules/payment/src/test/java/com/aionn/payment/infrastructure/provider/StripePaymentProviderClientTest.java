package com.aionn.payment.infrastructure.provider;

import com.aionn.payment.application.port.out.PaymentProviderClient;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import com.aionn.payment.infrastructure.provider.config.StripeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StripePaymentProviderClientTest {

    @Mock
    private StripeProperties properties;
    @Mock
    private MerchantQueryPort merchantQueryPort;

    private StripePaymentProviderClient client;

    @BeforeEach
    void setUp() {
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
        PaymentProviderClient.Refund refund = client.refund(
                new PaymentProviderClient.RefundRequest("p-1", "pi_test_abc", BigDecimal.TEN, "USD", "reason"));
        assertNotNull(refund);
        assertFalse(refund.accepted());
    }

    @Test
    void refundWithVndCurrencyZeroDecimalShouldAttemptStripeCall() {
        PaymentProviderClient.Refund refund = client.refund(
                new PaymentProviderClient.RefundRequest("p-1", "pi_test_abc",
                        BigDecimal.valueOf(100000), "VND", null));
        assertNotNull(refund);
        assertFalse(refund.accepted());
    }
}

