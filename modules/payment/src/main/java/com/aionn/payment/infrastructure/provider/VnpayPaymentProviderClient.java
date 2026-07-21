package com.aionn.payment.infrastructure.provider;

import com.aionn.payment.application.port.out.PaymentProviderClient;
import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import com.aionn.payment.infrastructure.provider.config.VnpayProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class VnpayPaymentProviderClient implements PaymentProviderClient {

    private static final String HMAC_SHA512 = "HmacSHA512";
    private static final DateTimeFormatter VNP_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final VnpayProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public PaymentGatewayKind kind() {
        return PaymentGatewayKind.VNPAY;
    }

    @Override
    public Authorization authorize(AuthorizationRequest request) {
        ensureConfigured();

        long vnpAmount = request.amount().multiply(BigDecimal.valueOf(100)).longValueExact();

        ZonedDateTime now = ZonedDateTime.now(VN_ZONE);
        String createDate = now.format(VNP_DATE_FMT);
        String expireDate = now.plusMinutes(15).format(VNP_DATE_FMT);

        String txnRef = request.paymentId();

        SortedMap<String, String> params = new TreeMap<>();
        params.put("vnp_Version", properties.version());
        params.put("vnp_Command", properties.command());
        params.put("vnp_TmnCode", properties.tmnCode());
        params.put("vnp_Amount", String.valueOf(vnpAmount));
        params.put("vnp_CurrCode", properties.currCode());
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", "Payment for order " + request.orderId());
        params.put("vnp_OrderType", "other");
        String vnpLocale = properties.locale();
        if (vnpLocale == null || vnpLocale.isBlank() || "vn".equalsIgnoreCase(vnpLocale)) {
            java.util.Locale current = org.springframework.context.i18n.LocaleContextHolder.getLocale();
            if (current != null && "en".equalsIgnoreCase(current.getLanguage())) {
                vnpLocale = "en";
            } else {
                vnpLocale = "vn";
            }
        }
        params.put("vnp_Locale", vnpLocale);
        params.put("vnp_ReturnUrl", request.returnUrl() != null && !request.returnUrl().isBlank()
                ? request.returnUrl()
                : properties.returnUrl());
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", expireDate);

        String hashData = buildQueryString(params);
        String secureHash = hmacSHA512(properties.hashSecret(), hashData);
        String paymentUrl = properties.payUrl() + "?" + hashData
                + "&vnp_SecureHash=" + urlEncode(secureHash);

        log.info("VNPay authorize: txnRef={} amount={} payUrl={}",
                txnRef, vnpAmount, properties.payUrl());
        log.debug("VNPay sign data: {}", hashData);
        log.debug("VNPay signature: {}", secureHash);

        return new Authorization(false, txnRef, paymentUrl, null, null);
    }

    @Override
    public WebhookEvent verifyAndParse(String rawBody, String signatureHeader) {
        ensureConfigured();

        Map<String, String> params = parseQueryString(rawBody);

        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) {
            return errorEvent("MISSING_SIGNATURE", "vnp_SecureHash is missing from callback");
        }

        SortedMap<String, String> sortedParams = new TreeMap<>(params);
        sortedParams.remove("vnp_SecureHash");
        sortedParams.remove("vnp_SecureHashType");

        String hashData = buildQueryString(sortedParams);
        String computedHash = hmacSHA512(properties.hashSecret(), hashData);

        if (!computedHash.equalsIgnoreCase(receivedHash)) {
            log.warn("VNPay signature mismatch: expected={} received={}", computedHash, receivedHash);
            return errorEvent("SIGNATURE_INVALID", "HMAC-SHA512 signature mismatch");
        }

        String responseCode = params.getOrDefault("vnp_ResponseCode", "99");
        String transactionStatus = params.getOrDefault("vnp_TransactionStatus", "99");
        String transactionNo = params.get("vnp_TransactionNo");
        String txnRef = params.get("vnp_TxnRef");
        String amountStr = params.get("vnp_Amount");

        BigDecimal amount = null;
        if (amountStr != null) {
            amount = new BigDecimal(amountStr).divide(BigDecimal.valueOf(100));
        }

        boolean success = "00".equals(responseCode) && "00".equals(transactionStatus);
        String errorCode = success ? null : "VNPAY_" + responseCode;
        String errorReason = success ? null : mapResponseCode(responseCode);

        log.info("VNPay callback: txnRef={} responseCode={} status={} success={}",
                txnRef, responseCode, transactionStatus, success);

        return new WebhookEvent(
                "vnpay.payment_result",
                txnRef,
                transactionNo,
                amount,
                properties.currCode(),
                success,
                errorCode,
                errorReason);
    }

    @Override
    public Refund refund(RefundRequest request) {
        ensureConfigured();

        long vnpAmount = request.amount().multiply(BigDecimal.valueOf(100)).longValueExact();
        ZonedDateTime now = ZonedDateTime.now(VN_ZONE);
        String createDate = now.format(VNP_DATE_FMT);
        String transactionDate = now.format(VNP_DATE_FMT);

        String requestId = "RF" + request.paymentId().substring(0, Math.min(16, request.paymentId().length()))
                + System.currentTimeMillis();
        String txnType = "02";
        String orderInfo = "Refund for payment " + request.paymentId()
                + (request.reason() == null ? "" : ": " + request.reason());

        String hashData = String.join("|",
                requestId,
                properties.version(),
                "refund",
                properties.tmnCode(),
                txnType,
                request.paymentId(),
                String.valueOf(vnpAmount),
                request.transactionNo() == null ? "" : request.transactionNo(),
                transactionDate,
                "system",
                createDate,
                "127.0.0.1",
                orderInfo);
        String secureHash = hmacSHA512(properties.hashSecret(), hashData);

        Map<String, String> body = new TreeMap<>();
        body.put("vnp_RequestId", requestId);
        body.put("vnp_Version", properties.version());
        body.put("vnp_Command", "refund");
        body.put("vnp_TmnCode", properties.tmnCode());
        body.put("vnp_TransactionType", txnType);
        body.put("vnp_TxnRef", request.paymentId());
        body.put("vnp_Amount", String.valueOf(vnpAmount));
        body.put("vnp_OrderInfo", orderInfo);
        body.put("vnp_TransactionNo", request.transactionNo() == null ? "" : request.transactionNo());
        body.put("vnp_TransactionDate", transactionDate);
        body.put("vnp_CreateBy", "system");
        body.put("vnp_CreateDate", createDate);
        body.put("vnp_IpAddr", "127.0.0.1");
        body.put("vnp_SecureHash", secureHash);

        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest http = HttpRequest.newBuilder()
                    .uri(URI.create(properties.apiUrl()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(http, HttpResponse.BodyHandlers.ofString());
            log.info("VNPay refund response status={} body={}", response.statusCode(), response.body());

            if (response.statusCode() != 200) {
                return new Refund(false, null, "VNPay HTTP " + response.statusCode());
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(response.body(), Map.class);
            String responseCode = String.valueOf(parsed.get("vnp_ResponseCode"));
            boolean accepted = "00".equals(responseCode);
            String message = parsed.get("vnp_Message") == null ? null : String.valueOf(parsed.get("vnp_Message"));
            String declineReason = accepted ? null
                    : (message != null ? message : "VNPay error " + responseCode);
            return new Refund(accepted, requestId, declineReason);
        } catch (Exception ex) {
            log.warn("VNPay refund failed: {}", ex.getMessage());
            throw new PaymentException(PaymentErrorCode.PAYMENT_GATEWAY_ERROR,
                    "Failed to call VNPay refund API: " + ex.getMessage());
        }
    }

    @Override
    public String generateInvoice(String paymentId, String orderId, BigDecimal amount, String currency) {
        return "https://sandbox.vnpayment.vn/transaction/" + paymentId;
    }

    private void ensureConfigured() {
        if (properties.tmnCode() == null || properties.tmnCode().isBlank()) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_GATEWAY_ERROR,
                    "VNPay TMN code is not configured (VNPAY_TMN_CODE)");
        }
        if (properties.hashSecret() == null || properties.hashSecret().isBlank()) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_GATEWAY_ERROR,
                    "VNPay hash secret is not configured (VNPAY_HASH_SECRET)");
        }
    }

    private static String buildQueryString(SortedMap<String, String> params) {
        return params.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> urlEncode(e.getKey()) + "=" + urlEncode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private static Map<String, String> parseQueryString(String query) {
        if (query == null || query.isBlank()) {
            return Map.of();
        }
        String cleaned = query.startsWith("?") ? query.substring(1) : query;
        return java.util.Arrays.stream(cleaned.split("&"))
                .map(pair -> pair.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(
                        parts -> urlDecode(parts[0]),
                        parts -> urlDecode(parts[1]),
                        (a, b) -> b));
    }

    static String hmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance(HMAC_SHA512);
            SecretKeySpec secretKey = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), HMAC_SHA512);
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_GATEWAY_ERROR,
                    "Failed to compute HMAC-SHA512: " + ex.getMessage());
        }
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String mapResponseCode(String code) {
        return switch (code) {
            case "00" -> "Success";
            case "07" -> "Transaction deducted but flagged as suspicious";
            case "09" -> "Card/Account not registered for Internet Banking";
            case "10" -> "Authentication failed more than 3 times";
            case "11" -> "Payment timeout";
            case "12" -> "Card/Account is locked";
            case "13" -> "Incorrect OTP";
            case "24" -> "Transaction cancelled by customer";
            case "51" -> "Insufficient balance";
            case "65" -> "Daily limit exceeded";
            case "75" -> "Bank is under maintenance";
            case "79" -> "Incorrect payment password (retry limit exceeded)";
            case "99" -> "Unknown error";
            default -> "VNPay error code " + code;
        };
    }

    private static WebhookEvent errorEvent(String errorCode, String reason) {
        return new WebhookEvent("vnpay.error", null, null, null, null, false, errorCode, reason);
    }
}
