package com.aionn.payment.infrastructure.provider;

import com.aionn.payment.application.port.out.PaymentProviderClient;
import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import com.aionn.payment.infrastructure.provider.config.VnpayProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VnpayPaymentProviderClientTest {

    @Mock
    private VnpayProperties properties;
    @Mock
    private ObjectMapper objectMapper;

    private VnpayPaymentProviderClient client;

    @BeforeEach
    void setUp() {
        client = new VnpayPaymentProviderClient(properties, objectMapper);
    }

    @Test
    void kindShouldReturnVnpay() {
        assertEquals(PaymentGatewayKind.VNPAY, client.kind());
    }

    @Test
    void authorizeShouldGenerateValidPaymentUrl() {
        when(properties.tmnCode()).thenReturn("TMN123");
        when(properties.version()).thenReturn("2.1.0");
        when(properties.hashSecret()).thenReturn("SECRET123");
        when(properties.payUrl()).thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");

        PaymentProviderClient.AuthorizationRequest request = new PaymentProviderClient.AuthorizationRequest(
                "pay-123", "order-456", "user-789", "merch-1", "pm-1", BigDecimal.valueOf(100000), "VND", "key-1", "http://return");

        PaymentProviderClient.Authorization auth = client.authorize(request);

        assertNotNull(auth);
        assertEquals("pay-123", auth.transactionNo());
        assertTrue(auth.authUrl().startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?"));
    }

    @Test
    void verifyAndParseShouldRejectMissingSignature() {
        when(properties.tmnCode()).thenReturn("TMN123");
        when(properties.hashSecret()).thenReturn("SECRET123");

        PaymentProviderClient.WebhookEvent event = client.verifyAndParse("vnp_Amount=10000", null);

        assertNotNull(event);
        assertEquals("MISSING_SIGNATURE", event.errorCode());
    }
}
