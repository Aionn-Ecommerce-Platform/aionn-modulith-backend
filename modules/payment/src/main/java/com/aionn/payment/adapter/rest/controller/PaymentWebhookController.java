package com.aionn.payment.adapter.rest.controller;

import com.aionn.payment.adapter.rest.mapper.payment.PaymentDtoMapper;
import com.aionn.payment.application.port.in.payment.ConfirmPaymentInputPort;
import com.aionn.payment.application.port.in.payment.FailPaymentInputPort;
import com.aionn.payment.application.port.out.PaymentProviderClient;
import com.aionn.payment.application.port.out.PaymentProviderRouter;
import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments/webhooks")
@RequiredArgsConstructor
@Tag(name = "Payment - Webhook", description = "Async callbacks from payment gateways")
public class PaymentWebhookController {

    private final PaymentProviderRouter providerRouter;
    private final ConfirmPaymentInputPort confirmPaymentInputPort;
    private final FailPaymentInputPort failPaymentInputPort;
    private final PaymentDtoMapper paymentDtoMapper;

    @PostMapping("/{gateway}")
    @Operation(summary = "Gateway webhook", description = "Verified + processed by the gateway-specific client")
    public ResponseEntity<Void> handle(
            @PathVariable String gateway,
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestBody String rawBody) {
        PaymentGatewayKind kind;
        try {
            kind = PaymentGatewayKind.valueOf(gateway.toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown gateway in webhook URL: {}", gateway);
            return ResponseEntity.badRequest().build();
        }
        PaymentProviderClient client = providerRouter.route(kind);
        PaymentProviderClient.WebhookEvent event = client.verifyAndParse(rawBody, signature);
        if (event.paymentId() == null) {
            log.warn("Webhook without paymentId for gateway {}", kind);
            return ResponseEntity.badRequest().build();
        }
        if (event.success()) {
            confirmPaymentInputPort.execute(paymentDtoMapper.toConfirmCommand(event));
        } else {
            failPaymentInputPort.execute(paymentDtoMapper.toFailCommand(event, "GATEWAY_ERROR"));
        }
        return ResponseEntity.ok().build();
    }
}
