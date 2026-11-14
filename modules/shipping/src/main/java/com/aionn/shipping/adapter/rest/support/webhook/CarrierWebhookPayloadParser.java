package com.aionn.shipping.adapter.rest.support.webhook;

import com.aionn.shipping.adapter.rest.dto.shipment.request.CarrierWebhookRequest;
import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Component
@RequiredArgsConstructor
public class CarrierWebhookPayloadParser {

    private final ObjectMapper objectMapper;

    public CarrierWebhookRequest parse(byte[] rawBody) {
        CarrierWebhookRequest request;
        try {
            request = objectMapper.readValue(rawBody, CarrierWebhookRequest.class);
        } catch (IOException ex) {
            log.warn("Malformed carrier webhook payload: {}", ex.getMessage());
            throw new ShippingException(ShippingErrorCode.INVALID_ARGUMENT,
                    "Malformed webhook payload");
        }
        if (request == null
                || request.trackingCode() == null || request.trackingCode().isBlank()
                || request.type() == null || request.type().isBlank()) {
            throw new ShippingException(ShippingErrorCode.INVALID_ARGUMENT,
                    "Webhook requires trackingCode and type");
        }
        return request;
    }

    public String eventId(byte[] rawBody) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawBody));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
