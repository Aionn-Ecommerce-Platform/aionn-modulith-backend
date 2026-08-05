package com.aionn.shipping.adapter.rest.controller;

import com.aionn.shipping.adapter.rest.dto.shipment.CarrierWebhookRequest;
import com.aionn.shipping.adapter.rest.mapper.shipment.ShipmentDtoMapper;
import com.aionn.shipping.application.dto.shipment.command.CarrierWebhookCommand;
import com.aionn.shipping.application.port.in.shipment.ApplyCarrierWebhookInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
        private final ShipmentDtoMapper shipmentDtoMapper;

        @PostMapping("/carrier")
        @Operation(summary = "Carrier webhook")
        public ResponseEntity<ApiResponse<Void>> carrierWebhook(
                        @RequestHeader(name = "X-Webhook-Secret", required = false) String secret,
                        @Valid @RequestBody CarrierWebhookRequest request) {
                CarrierWebhookCommand command = shipmentDtoMapper.toCommand(request, secret);
                applyCarrierWebhookInputPort.execute(command);
                return ResponseEntity.ok(ApiResponse.success(null, "Webhook applied"));
        }
}
