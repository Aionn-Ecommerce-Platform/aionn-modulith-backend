package com.aionn.shipping.adapter.rest.controller;

import com.aionn.shipping.adapter.rest.dto.shipment.request.CarrierWebhookRequest;
import com.aionn.shipping.adapter.rest.mapper.shipment.ShippingWebhookDtoMapper;
import com.aionn.shipping.adapter.rest.support.webhook.CarrierWebhookPayloadParser;
import com.aionn.shipping.application.dto.shipment.command.CarrierWebhookCommand;
import com.aionn.shipping.application.port.in.shipment.ApplyCarrierWebhookInputPort;
import com.aionn.platform.web.response.ApiResponse;
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
@RequestMapping("/api/v1/shipping/webhooks")
@RequiredArgsConstructor
@Tag(name = "Shipping - Webhook", description = "Carrier webhook entry point")
public class ShippingWebhookController {

    private final ApplyCarrierWebhookInputPort applyCarrierWebhookInputPort;
    private final ShippingWebhookDtoMapper webhookDtoMapper;
    private final CarrierWebhookPayloadParser payloadParser;

    @PostMapping("/carrier")
    @Operation(summary = "Carrier webhook")
    public ResponseEntity<ApiResponse<Void>> carrierWebhook(
            @RequestHeader(name = "X-Webhook-Secret", required = false) String legacySecret,
            @RequestHeader(name = "X-Webhook-Signature", required = false) String signature,
            @RequestHeader(name = "X-Webhook-Timestamp", required = false) String timestamp,
            @RequestHeader(name = "X-Event-Id", required = false) String eventId,
            @RequestBody byte[] rawBody) {
        CarrierWebhookRequest request = payloadParser.parse(rawBody);
        String effectiveEventId = eventId == null || eventId.isBlank()
                ? payloadParser.eventId(rawBody)
                : eventId;
        CarrierWebhookCommand command = webhookDtoMapper.toCommand(
                request, legacySecret, rawBody, signature, timestamp, effectiveEventId);
        applyCarrierWebhookInputPort.execute(command);
        return ResponseEntity.ok(ApiResponse.success(null, "Webhook applied"));
    }
}
