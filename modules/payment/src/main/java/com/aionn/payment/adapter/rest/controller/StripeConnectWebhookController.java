package com.aionn.payment.adapter.rest.controller;

import com.aionn.payment.application.port.in.stripeconnect.SyncStripeConnectAccountInputPort;
import com.aionn.payment.application.port.out.stripeconnect.StripeConnectWebhookPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments/webhooks/stripe-connect")
@RequiredArgsConstructor
@Tag(name = "Payment - Stripe Connect Webhook", description = "Stripe Connect account capability updates")
public class StripeConnectWebhookController {

    private final StripeConnectWebhookPort stripeConnectWebhookPort;
    private final SyncStripeConnectAccountInputPort syncStripeConnectAccountInputPort;

    @PostMapping
    @Operation(summary = "Stripe Connect webhook (account.updated)")
    public ResponseEntity<Void> handle(
            @RequestHeader(value = "Stripe-Signature", required = false) String signature,
            @RequestBody String rawBody) {
        StripeConnectWebhookPort.WebhookEvent event = stripeConnectWebhookPort.parseAndVerify(rawBody, signature);
        if (event == null) {
            return ResponseEntity.badRequest().build();
        }
        if (!"account.updated".equals(event.type())) {
            return ResponseEntity.ok().build();
        }
        if (event.stripeAccountId() == null) {
            log.warn("Stripe Connect webhook payload could not be parsed");
            return ResponseEntity.badRequest().build();
        }
        syncStripeConnectAccountInputPort.execute(event.stripeAccountId(), event.chargesEnabled(), event.payoutsEnabled());
        return ResponseEntity.ok().build();
    }
}
