package com.aionn.payment.adapter.rest.controller;

import com.aionn.payment.application.service.StripeConnectWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/webhooks/stripe-connect")
@RequiredArgsConstructor
@Tag(name = "Payment - Stripe Connect Webhook", description = "Stripe Connect account capability updates")
public class StripeConnectWebhookController {

    private final StripeConnectWebhookService stripeConnectWebhookService;

    @PostMapping
    @Operation(summary = "Stripe Connect webhook (account.updated)")
    public ResponseEntity<Void> handle(
            @RequestHeader(value = "Stripe-Signature", required = false) String signature,
            @RequestBody byte[] rawBody) {
        return stripeConnectWebhookService.handle(rawBody, signature)
                ? ResponseEntity.ok().build()
                : ResponseEntity.badRequest().build();
    }
}
