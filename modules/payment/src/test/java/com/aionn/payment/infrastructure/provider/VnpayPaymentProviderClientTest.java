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
        when(properties.command()).thenReturn("pay");
        when(properties.currCode()).thenReturn("VND");

        PaymentProviderClient.AuthorizationRequest request = new PaymentProviderClient.AuthorizationRequest(
                "pay-123", "order-456", "user-789", "merch-1", "pm-1", BigDecimal.valueOf(100000), "VND", "key-1", "http://return");

        PaymentProviderClient.Authorization auth = client.authorize(request);

        assertNotNull(auth);
        assertEquals("pay-123", auth.transactionNo());
        assertFalse(auth.captured());
        assertTrue(auth.authUrl().startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?"));
    }

    @Test
    void authorizeShouldThrowWhenNotConfigured() {
        when(properties.tmnCode()).thenReturn(null);

        assertThrows(com.aionn.payment.domain.exception.PaymentException.class,
                () -> client.authorize(new PaymentProviderClient.AuthorizationRequest(
                        "pay-1", "order-1", "user-1", null, null, BigDecimal.TEN, "VND", "k", null)));
    }

    @Test
    void verifyAndParseShouldRejectMissingSignature() {
        when(properties.tmnCode()).thenReturn("TMN123");
        when(properties.hashSecret()).thenReturn("SECRET123");

        PaymentProviderClient.WebhookEvent event = client.verifyAndParse("vnp_Amount=10000", null);

        assertNotNull(event);
        assertEquals("MISSING_SIGNATURE", event.errorCode());
    }

    @Test
    void verifyAndParseShouldRejectInvalidSignature() {
        when(properties.tmnCode()).thenReturn("TMN123");
        when(properties.hashSecret()).thenReturn("SECRET123");

        String body = "vnp_Amount=10000&vnp_TxnRef=pay-1&vnp_ResponseCode=00&vnp_SecureHash=INVALID_HASH";
        PaymentProviderClient.WebhookEvent event = client.verifyAndParse(body, null);

        assertNotNull(event);
        assertEquals("SIGNATURE_INVALID", event.errorCode());
    }

    @Test
    void verifyAndParseShouldReturnSuccessEventForValidSignature() {
        String secret = "TESTSECRET";
        when(properties.tmnCode()).thenReturn("TMN123");
        when(properties.hashSecret()).thenReturn(secret);
        when(properties.currCode()).thenReturn("VND");

        java.util.SortedMap<String, String> params = new java.util.TreeMap<>();
        params.put("vnp_Amount", "10000000");
        params.put("vnp_TxnRef", "pay-123");
        params.put("vnp_TransactionNo", "txn-456");
        params.put("vnp_ResponseCode", "00");

        String hashData = params.entrySet().stream()
                .map(e -> java.net.URLEncoder.encode(e.getKey(), java.nio.charset.StandardCharsets.UTF_8)
                        + "=" + java.net.URLEncoder.encode(e.getValue(), java.nio.charset.StandardCharsets.UTF_8))
                .collect(java.util.stream.Collectors.joining("&"));
        String computedHash = VnpayPaymentProviderClient.hmacSHA512(secret, hashData);

        String body = hashData + "&vnp_SecureHash=" + java.net.URLEncoder.encode(computedHash, java.nio.charset.StandardCharsets.UTF_8);
        PaymentProviderClient.WebhookEvent event = client.verifyAndParse(body, null);

        assertNotNull(event);
        assertTrue(event.success());
        assertEquals("pay-123", event.paymentId());
        assertEquals("txn-456", event.transactionNo());
    }

    @Test
    void verifyAndParseShouldReturnFailedEventForNonZeroResponseCode() {
        String secret = "TESTSECRET";
        when(properties.tmnCode()).thenReturn("TMN123");
        when(properties.hashSecret()).thenReturn(secret);
        when(properties.currCode()).thenReturn("VND");

        java.util.SortedMap<String, String> params = new java.util.TreeMap<>();
        params.put("vnp_Amount", "10000000");
        params.put("vnp_TxnRef", "pay-123");
        params.put("vnp_ResponseCode", "24");

        String hashData = params.entrySet().stream()
                .map(e -> java.net.URLEncoder.encode(e.getKey(), java.nio.charset.StandardCharsets.UTF_8)
                        + "=" + java.net.URLEncoder.encode(e.getValue(), java.nio.charset.StandardCharsets.UTF_8))
                .collect(java.util.stream.Collectors.joining("&"));
        String computedHash = VnpayPaymentProviderClient.hmacSHA512(secret, hashData);

        String body = hashData + "&vnp_SecureHash=" + java.net.URLEncoder.encode(computedHash, java.nio.charset.StandardCharsets.UTF_8);
        PaymentProviderClient.WebhookEvent event = client.verifyAndParse(body, null);

        assertNotNull(event);
        assertFalse(event.success());
        assertEquals("VNPAY_24", event.errorCode());
    }

    @Test
    void generateInvoiceShouldReturnFormattedUrl() {
        String url = client.generateInvoice("pay-123", "order-456", BigDecimal.TEN, "VND");
        assertNotNull(url);
        assertTrue(url.contains("pay-123"));
    }

    @Test
    void hmacSHA512ShouldReturnConsistentHash() {
        String hash1 = VnpayPaymentProviderClient.hmacSHA512("secret", "data");
        String hash2 = VnpayPaymentProviderClient.hmacSHA512("secret", "data");
        assertEquals(hash1, hash2);
        assertFalse(hash1.isEmpty());
    }

    @Test
    void verifyAndParseShouldHandleEmptyBody() {
        when(properties.tmnCode()).thenReturn("TMN123");
        when(properties.hashSecret()).thenReturn("SECRET");

        PaymentProviderClient.WebhookEvent event = client.verifyAndParse("", null);
        assertNotNull(event);
        assertEquals("MISSING_SIGNATURE", event.errorCode());
    }

    @Test
    void authorizeShouldThrowWhenHashSecretNotConfigured() {
        when(properties.tmnCode()).thenReturn("TMN123");
        when(properties.hashSecret()).thenReturn(null);

        assertThrows(com.aionn.payment.domain.exception.PaymentException.class,
                () -> client.authorize(new PaymentProviderClient.AuthorizationRequest(
                        "pay-1", "order-1", "user-1", null, null, BigDecimal.TEN, "VND", "k", null)));
    }

    @Test
    void authorizeShouldUseDefaultReturnUrlWhenNotProvidedInRequest() {
        when(properties.tmnCode()).thenReturn("TMN123");
        when(properties.version()).thenReturn("2.1.0");
        when(properties.hashSecret()).thenReturn("SECRET");
        when(properties.payUrl()).thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        when(properties.command()).thenReturn("pay");
        when(properties.currCode()).thenReturn("VND");
        when(properties.locale()).thenReturn("vn");
        when(properties.returnUrl()).thenReturn("http://default-return");

        PaymentProviderClient.AuthorizationRequest request = new PaymentProviderClient.AuthorizationRequest(
                "pay-999", "order-999", "user-1", null, null, BigDecimal.valueOf(50000), "VND", "k", null);

        PaymentProviderClient.Authorization auth = client.authorize(request);

        assertNotNull(auth);
        assertEquals("pay-999", auth.transactionNo());
    }

    @Test
    void authorizeShouldUseEnLocaleWhenConfigured() {
        when(properties.tmnCode()).thenReturn("TMN123");
        when(properties.version()).thenReturn("2.1.0");
        when(properties.hashSecret()).thenReturn("SECRET");
        when(properties.payUrl()).thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        when(properties.command()).thenReturn("pay");
        when(properties.currCode()).thenReturn("VND");
        when(properties.locale()).thenReturn("en");

        PaymentProviderClient.AuthorizationRequest request = new PaymentProviderClient.AuthorizationRequest(
                "pay-888", "order-888", "user-1", null, null, BigDecimal.valueOf(50000), "VND", "k", "http://ret");

        PaymentProviderClient.Authorization auth = client.authorize(request);
        assertNotNull(auth);
        assertTrue(auth.authUrl().contains("vnp_Locale=en"));
    }

    @Test
    void refundShouldReturnAcceptedWhenVnpayRespondsSuccessfully() throws Exception {
        when(properties.tmnCode()).thenReturn("TMN123");
        when(properties.hashSecret()).thenReturn("SECRET");
        when(properties.version()).thenReturn("2.1.0");
        when(properties.apiUrl()).thenReturn("https://sandbox.vnpayment.vn/merchant_webapi/api/transaction");

        String successBody = "{\"vnp_ResponseCode\":\"00\",\"vnp_Message\":\"Approve Success\"}";
        java.net.http.HttpResponse<String> mockResponse = mockHttpResponse(200, successBody);
        java.net.http.HttpClient mockHttpClient = mockHttpClient(mockResponse);
        injectHttpClient(client, mockHttpClient);

        when(objectMapper.writeValueAsString(org.mockito.ArgumentMatchers.any())).thenReturn("{}");
        when(objectMapper.readValue(successBody, java.util.Map.class))
                .thenReturn(java.util.Map.of("vnp_ResponseCode", "00", "vnp_Message", "Approve Success"));

        PaymentProviderClient.Refund refund = client.refund(
                new PaymentProviderClient.RefundRequest("pay-1", "txn-001", BigDecimal.valueOf(100000), "VND", null));

        assertNotNull(refund);
        assertTrue(refund.accepted());
    }

    @Test
    void refundShouldReturnDeclinedWhenVnpayRespondsFailed() throws Exception {
        when(properties.tmnCode()).thenReturn("TMN123");
        when(properties.hashSecret()).thenReturn("SECRET");
        when(properties.version()).thenReturn("2.1.0");
        when(properties.apiUrl()).thenReturn("https://sandbox.vnpayment.vn/merchant_webapi/api/transaction");

        String failBody = "{\"vnp_ResponseCode\":\"02\",\"vnp_Message\":\"Order already refunded\"}";
        java.net.http.HttpResponse<String> mockResponse = mockHttpResponse(200, failBody);
        java.net.http.HttpClient mockHttpClient = mockHttpClient(mockResponse);
        injectHttpClient(client, mockHttpClient);

        when(objectMapper.writeValueAsString(org.mockito.ArgumentMatchers.any())).thenReturn("{}");
        when(objectMapper.readValue(failBody, java.util.Map.class))
                .thenReturn(java.util.Map.of("vnp_ResponseCode", "02", "vnp_Message", "Order already refunded"));

        PaymentProviderClient.Refund refund = client.refund(
                new PaymentProviderClient.RefundRequest("pay-1", "txn-001", BigDecimal.valueOf(100000), "VND", "test"));

        assertNotNull(refund);
        assertFalse(refund.accepted());
    }

    @Test
    void refundShouldReturnDeclinedWhenVnpayHttpNon200() throws Exception {
        when(properties.tmnCode()).thenReturn("TMN123");
        when(properties.hashSecret()).thenReturn("SECRET");
        when(properties.version()).thenReturn("2.1.0");
        when(properties.apiUrl()).thenReturn("https://sandbox.vnpayment.vn/merchant_webapi/api/transaction");

        java.net.http.HttpResponse<String> mockResponse = mockHttpResponse(500, "Internal Error");
        java.net.http.HttpClient mockHttpClient = mockHttpClient(mockResponse);
        injectHttpClient(client, mockHttpClient);

        when(objectMapper.writeValueAsString(org.mockito.ArgumentMatchers.any())).thenReturn("{}");

        PaymentProviderClient.Refund refund = client.refund(
                new PaymentProviderClient.RefundRequest("pay-1", "txn-001", BigDecimal.valueOf(100000), "VND", null));

        assertNotNull(refund);
        assertFalse(refund.accepted());
    }

    @SuppressWarnings("unchecked")
    private static java.net.http.HttpResponse<String> mockHttpResponse(int statusCode, String body) {
        java.net.http.HttpResponse<String> response = org.mockito.Mockito.mock(java.net.http.HttpResponse.class);
        org.mockito.Mockito.when(response.statusCode()).thenReturn(statusCode);
        org.mockito.Mockito.when(response.body()).thenReturn(body);
        return response;
    }

    @SuppressWarnings("unchecked")
    private static java.net.http.HttpClient mockHttpClient(java.net.http.HttpResponse<String> response) throws Exception {
        java.net.http.HttpClient httpClient = org.mockito.Mockito.mock(java.net.http.HttpClient.class);
        org.mockito.Mockito.when(httpClient.send(
                org.mockito.ArgumentMatchers.any(java.net.http.HttpRequest.class),
                org.mockito.ArgumentMatchers.any(java.net.http.HttpResponse.BodyHandler.class)))
                .thenReturn(response);
        return httpClient;
    }

    private static void injectHttpClient(VnpayPaymentProviderClient client, java.net.http.HttpClient httpClient)
            throws Exception {
        java.lang.reflect.Field field = VnpayPaymentProviderClient.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        field.set(client, httpClient);
    }
}
