package com.aionn.payment.adapter.rest.controller;

import com.aionn.payment.adapter.rest.dto.vnpay.response.VnpayIpnResponse;
import com.aionn.payment.application.service.VnpayWebhookService;
import com.aionn.payment.infrastructure.provider.config.VnpayProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/payments/vnpay")
@RequiredArgsConstructor
@Tag(name = "Payment - VNPay", description = "VNPay gateway callback endpoints")
public class VnpayReturnController {

    private final VnpayWebhookService vnpayWebhookService;
    private final VnpayProperties vnpayProperties;

    @GetMapping("/return")
    @Operation(summary = "VNPay redirect callback")
    public ResponseEntity<Void> handleReturn(HttpServletRequest request) {
        VnpayWebhookService.ReturnResult result = vnpayWebhookService.handleReturn(request.getQueryString());
        if (result.status() == VnpayWebhookService.ReturnStatus.BAD_REQUEST) {
            return ResponseEntity.badRequest().build();
        }
        if (result.status() == VnpayWebhookService.ReturnStatus.INTERNAL_ERROR) {
            return ResponseEntity.internalServerError().build();
        }
        URI redirect = UriComponentsBuilder.fromUriString(vnpayProperties.frontendReturnUrl())
                .encode()
                .queryParam("paymentId", "{paymentId}")
                .queryParam("orderId", "{orderId}")
                .buildAndExpand(result.paymentId(), result.orderId())
                .toUri();
        return ResponseEntity.status(HttpStatus.FOUND).location(redirect).build();
    }

    @PostMapping("/ipn")
    @Operation(summary = "VNPay IPN callback")
    public ResponseEntity<VnpayIpnResponse> handleIpn(HttpServletRequest request) {
        VnpayWebhookService.IpnResult result = vnpayWebhookService.handleIpn(request.getQueryString());
        return ResponseEntity.ok(new VnpayIpnResponse(
                result.code().code(), result.code().message()));
    }
}
