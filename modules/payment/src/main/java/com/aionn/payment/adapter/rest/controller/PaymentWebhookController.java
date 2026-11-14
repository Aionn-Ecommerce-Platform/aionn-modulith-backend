package com.aionn.payment.adapter.rest.controller;

import com.aionn.payment.application.service.PaymentWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/webhooks")
@RequiredArgsConstructor
@Tag(name = "Payment - Webhook", description = "Async callbacks from payment gateways")
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    @PostMapping("/{gateway}")
    @Operation(summary = "Gateway webhook", description = "Verified + processed by the gateway-specific client")
    public ResponseEntity<Void> handle(
            @PathVariable String gateway,
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestBody byte[] rawBody) {
        return paymentWebhookService.handle(gateway, rawBody, signature)
                ? ResponseEntity.ok().build()
                : ResponseEntity.badRequest().build();
    }
}
