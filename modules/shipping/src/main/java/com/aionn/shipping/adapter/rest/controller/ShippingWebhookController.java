package com.aionn.shipping.adapter.rest.controller;

import com.aionn.shipping.adapter.rest.dto.shipment.request.CarrierWebhookRequest;
import com.aionn.shipping.adapter.rest.mapper.shipment.ShippingWebhookDtoMapper;
import com.aionn.shipping.application.dto.shipment.command.CarrierWebhookCommand;
import com.aionn.shipping.application.port.in.shipment.ApplyCarrierWebhookInputPort;
import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import com.aionn.platform.web.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@RestController
@RequestMapping("/api/v1/shipping/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Shipping - Webhook", description = "Carrier webhook entry point")
public class ShippingWebhookController {

        private final ApplyCarrierWebhookInputPort applyCarrierWebhookInputPort;
        private final ShippingWebhookDtoMapper webhookDtoMapper;
        private final ObjectMapper objectMapper;

        @PostMapping("/carrier")
        @Operation(summary = "Carrier webhook")
        public ResponseEntity<ApiResponse<Void>> carrierWebhook(
                        @RequestHeader(name = "X-Webhook-Secret", required = false) String legacySecret,
                        @RequestHeader(name = "X-Webhook-Signature", required = false) String signature,
                        @RequestHeader(name = "X-Webhook-Timestamp", required = false) String timestamp,
                        @RequestHeader(name = "X-Event-Id", required = false) String eventId,
                        @RequestBody byte[] rawBody) {
                String rawPayload = new String(rawBody, StandardCharsets.UTF_8);
                CarrierWebhookRequest request = parse(rawPayload);
                String effectiveEventId = eventId == null || eventId.isBlank()
                                ? payloadDigest(rawPayload)
                                : eventId;
                CarrierWebhookCommand command = webhookDtoMapper.toCommand(
                                request, legacySecret, rawPayload, signature, timestamp, effectiveEventId);
                applyCarrierWebhookInputPort.execute(command);
                return ResponseEntity.ok(ApiResponse.success(null, "Webhook applied"));
        }

        private CarrierWebhookRequest parse(String rawPayload) {
                try {
                        CarrierWebhookRequest request = objectMapper.readValue(rawPayload, CarrierWebhookRequest.class);
                        if (request == null
                                        || request.trackingCode() == null || request.trackingCode().isBlank()
                                        || request.type() == null || request.type().isBlank()) {
                                throw new ShippingException(ShippingErrorCode.INVALID_ARGUMENT,
                                                "Webhook requires trackingCode and type");
                        }
                        return request;
                } catch (ShippingException ex) {
                        throw ex;
                } catch (Exception ex) {
                        log.warn("Malformed carrier webhook payload: {}", ex.getMessage());
                        throw new ShippingException(ShippingErrorCode.INVALID_ARGUMENT,
                                        "Malformed webhook payload");
                }
        }

        /** Carriers that do not send an event id still need duplicate deliveries suppressed. */
        private static String payloadDigest(String rawPayload) {
                try {
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        return HexFormat.of().formatHex(
                                        digest.digest(rawPayload.getBytes(StandardCharsets.UTF_8)));
                } catch (NoSuchAlgorithmException ex) {
                        throw new IllegalStateException("SHA-256 is not available", ex);
                }
        }
}
