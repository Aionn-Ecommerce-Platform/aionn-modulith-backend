package com.aionn.payment.adapter.rest.controller;

import com.aionn.payment.adapter.rest.mapper.payment.PaymentDtoMapper;
import com.aionn.payment.application.dto.payment.result.PaymentResult;
import com.aionn.payment.application.port.in.payment.ConfirmPaymentInputPort;
import com.aionn.payment.application.port.in.payment.FailPaymentInputPort;
import com.aionn.payment.application.port.in.payment.GetPaymentInputPort;
import com.aionn.payment.application.port.out.PaymentProviderClient;
import com.aionn.payment.application.port.out.PaymentProviderRouter;
import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import com.aionn.payment.infrastructure.provider.config.VnpayProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments/vnpay")
@RequiredArgsConstructor
@Tag(name = "Payment - VNPay", description = "VNPay gateway callback endpoints")
public class VnpayReturnController {

    private static final String PAYMENT_ID_KEY = "paymentId";
    private static final String RSP_CODE_KEY = "RspCode";
    private static final String MESSAGE_KEY = "Message";

    private final PaymentProviderRouter providerRouter;
    private final GetPaymentInputPort getPaymentInputPort;
    private final ConfirmPaymentInputPort confirmPaymentInputPort;
    private final FailPaymentInputPort failPaymentInputPort;
    private final VnpayProperties vnpayProperties;
    private final PaymentDtoMapper paymentDtoMapper;

    @GetMapping("/return")
    @Operation(summary = "VNPay redirect callback")
    public ResponseEntity<Object> handleReturn(HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> result = finalisePayment(request.getQueryString());
        Map<String, Object> body = result.getBody();
        if (body == null || body.get(PAYMENT_ID_KEY) == null) {
            return ResponseEntity.status(result.getStatusCode()).body(body);
        }
        String paymentId = String.valueOf(body.get(PAYMENT_ID_KEY));
        PaymentResult payment = getPaymentInputPort.execute(paymentId);
        String separator = vnpayProperties.frontendReturnUrl().contains("?") ? "&" : "?";
        URI redirect = URI.create(vnpayProperties.frontendReturnUrl() + separator
                + "paymentId=" + paymentId + "&orderId=" + payment.orderId());
        return ResponseEntity.status(HttpStatus.FOUND).location(redirect).build();
    }

    @PostMapping("/ipn")
    @Operation(summary = "VNPay IPN callback")
    public ResponseEntity<Map<String, String>> handleIpn(HttpServletRequest request) {
        PaymentProviderClient client = providerRouter.route(PaymentGatewayKind.VNPAY);
        PaymentProviderClient.WebhookEvent event = client.verifyAndParse(request.getQueryString(), null);

        Map<String, String> response = new LinkedHashMap<>();
        if (event.paymentId() == null) {
            response.put(RSP_CODE_KEY, "97");
            response.put(MESSAGE_KEY, "Invalid Signature");
            return ResponseEntity.ok(response);
        }
        try {
            PaymentResult current = getPaymentInputPort.execute(event.paymentId());
            if (!"PAID".equals(current.status()) && !"FAILED".equals(current.status())) {
                if (event.success()) {
                    confirmPaymentInputPort.execute(paymentDtoMapper.toConfirmCommand(event));
                } else {
                    failPaymentInputPort.execute(paymentDtoMapper.toFailCommand(event, "VNPAY_ERROR"));
                }
            }
            response.put(RSP_CODE_KEY, "00");
            response.put(MESSAGE_KEY, "Confirm Success");
        } catch (PaymentException ex) {
            if (PaymentErrorCode.PAYMENT_NOT_FOUND.getCode().equals(ex.getErrorCode())) {
                response.put(RSP_CODE_KEY, "01");
                response.put(MESSAGE_KEY, "Order not Found");
            } else {
                response.put(RSP_CODE_KEY, "99");
                response.put(MESSAGE_KEY, "Unknown error");
            }
        }
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> finalisePayment(String query) {
        PaymentProviderClient client = providerRouter.route(PaymentGatewayKind.VNPAY);
        PaymentProviderClient.WebhookEvent event = client.verifyAndParse(query, null);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paymentId", event.paymentId());
        body.put("transactionNo", event.transactionNo());
        body.put("success", event.success());
        if (!event.success()) {
            body.put("errorCode", event.errorCode());
            body.put("errorReason", event.errorReason());
        }

        if (event.paymentId() == null) {
            log.warn("VNPay return without paymentId, query={}", query);
            return ResponseEntity.badRequest().body(body);
        }

        try {
            PaymentResult current = getPaymentInputPort.execute(event.paymentId());
            if (!"PAID".equals(current.status()) && !"FAILED".equals(current.status())) {
                if (event.success()) {
                    confirmPaymentInputPort.execute(paymentDtoMapper.toConfirmCommand(event));
                } else {
                    failPaymentInputPort.execute(paymentDtoMapper.toFailCommand(event, "VNPAY_ERROR"));
                }
            }
        } catch (Exception ex) {
            log.error("VNPay return finalisation failed for {}", event.paymentId(), ex);
            body.put("error", ex.getMessage());
            return ResponseEntity.internalServerError().body(body);
        }
        return ResponseEntity.ok(body);
    }
}
