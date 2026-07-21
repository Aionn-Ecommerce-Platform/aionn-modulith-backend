package com.aionn.payment.adapter.rest.controller;

import com.aionn.payment.adapter.rest.mapper.payment.PaymentDtoMapper;
import com.aionn.payment.application.dto.payment.result.PaymentResult;
import com.aionn.payment.application.port.in.payment.ConfirmPaymentInputPort;
import com.aionn.payment.application.port.in.payment.FailPaymentInputPort;
import com.aionn.payment.application.port.in.payment.GetPaymentInputPort;
import com.aionn.payment.application.port.out.PaymentProviderClient;
import com.aionn.payment.application.port.out.PaymentProviderRouter;
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
@Tag(name = "Payment - VNPay return", description = "VNPay redirect + IPN callback")
public class VnpayReturnController {

    private final PaymentProviderRouter providerRouter;
    private final GetPaymentInputPort getPaymentInputPort;
    private final ConfirmPaymentInputPort confirmPaymentInputPort;
    private final FailPaymentInputPort failPaymentInputPort;
    private final VnpayProperties vnpayProperties;
    private final PaymentDtoMapper paymentDtoMapper;

    @GetMapping("/return")
    @Operation(summary = "VNPay redirect callback")
    public ResponseEntity<?> handleReturn(HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> result = finalisePayment(request.getQueryString());
        Map<String, Object> body = result.getBody();
        if (body == null || body.get("paymentId") == null) {
            return result;
        }
        String paymentId = String.valueOf(body.get("paymentId"));
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
            response.put("RspCode", "97");
            response.put("Message", "Invalid Signature");
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
            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
        } catch (Exception ex) {
            log.error("VNPay IPN finalisation failed for {}", event.paymentId(), ex);
            response.put("RspCode", "99");
            response.put("Message", "Unknown error");
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
