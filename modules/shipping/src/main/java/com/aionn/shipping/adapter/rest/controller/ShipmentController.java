package com.aionn.shipping.adapter.rest.controller;

import com.aionn.shipping.adapter.rest.dto.shipment.CancelShipmentRequest;
import com.aionn.shipping.adapter.rest.dto.shipment.CreateShipmentRequest;
import com.aionn.shipping.adapter.rest.dto.shipment.QuoteShippingRequest;
import com.aionn.shipping.adapter.rest.dto.shipment.ResolveIssueRequest;
import com.aionn.shipping.adapter.rest.dto.shipment.response.ShipmentResponse;
import com.aionn.shipping.adapter.rest.dto.rate.response.ShippingQuoteResponse;
import com.aionn.shipping.adapter.rest.mapper.shipment.ShipmentDtoMapper;
import com.aionn.shipping.adapter.rest.mapper.rate.ShippingRateDtoMapper;
import com.aionn.shipping.adapter.rest.support.session.CurrentUserId;
import com.aionn.shipping.application.dto.shipment.command.CancelShipmentCommand;
import com.aionn.shipping.application.dto.shipment.command.CreateShipmentCommand;
import com.aionn.shipping.application.dto.shipment.command.FetchLabelCommand;
import com.aionn.shipping.application.dto.shipment.command.QuoteShippingCommand;
import com.aionn.shipping.application.dto.shipment.command.ResolveIssueCommand;
import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;
import com.aionn.shipping.application.port.in.shipment.QuoteShippingInputPort;
import com.aionn.shipping.application.port.in.shipment.CreateShipmentInputPort;
import com.aionn.shipping.application.port.in.shipment.RegisterShipmentInputPort;
import com.aionn.shipping.application.port.in.shipment.FetchLabelInputPort;
import com.aionn.shipping.application.port.in.shipment.CancelShipmentInputPort;
import com.aionn.shipping.application.port.in.shipment.ResolveIssueInputPort;
import com.aionn.shipping.application.port.in.shipment.GetShipmentInputPort;
import com.aionn.shipping.application.port.in.shipment.ListShipmentsByOrderInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shipping/shipments")
@RequiredArgsConstructor
@Tag(name = "Shipping - Shipment", description = "Shipment lifecycle endpoints")
public class ShipmentController {

        private final QuoteShippingInputPort quoteShippingInputPort;
        private final CreateShipmentInputPort createShipmentInputPort;
        private final RegisterShipmentInputPort registerShipmentInputPort;
        private final FetchLabelInputPort fetchLabelInputPort;
        private final CancelShipmentInputPort cancelShipmentInputPort;
        private final ResolveIssueInputPort resolveIssueInputPort;
        private final GetShipmentInputPort getShipmentInputPort;
        private final ListShipmentsByOrderInputPort listShipmentsByOrderInputPort;
        private final ShipmentDtoMapper shipmentDtoMapper;
        private final ShippingRateDtoMapper shippingRateDtoMapper;

        @PostMapping("/quote")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Quote shipping fee",
                        description = "orderId is optional so checkout can request a quote before creating an order")
        public ResponseEntity<ApiResponse<ShippingQuoteResponse>> quote(
                        @Valid @RequestBody QuoteShippingRequest request) {
                QuoteShippingCommand command = shippingRateDtoMapper.toCommand(request);
                return ResponseEntity.ok(ApiResponse.success(
                                shippingRateDtoMapper.toResponse(quoteShippingInputPort.execute(command)),
                                "Quote computed"));
        }

        @PostMapping
        @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
        @Operation(summary = "Create shipment")
        public ResponseEntity<ApiResponse<ShipmentResponse>> create(
                        @CurrentUserId String ownerId,
                        @Valid @RequestBody CreateShipmentRequest request) {
                CreateShipmentCommand command = shipmentDtoMapper.toCommand(request, ownerId);
                return ApiResponse.createdResponse("Shipment created",
                                shipmentDtoMapper.toResponse(createShipmentInputPort.execute(command)));
        }

        @PostMapping("/{shipmentId}/register")
        @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
        @Operation(summary = "Register shipment with carrier")
        public ResponseEntity<ApiResponse<ShipmentResponse>> register(@PathVariable String shipmentId) {
                ShipmentResult result = registerShipmentInputPort.execute(shipmentId);
                return ResponseEntity.ok(ApiResponse.success(
                                shipmentDtoMapper.toResponse(result),
                                "Shipment registered with carrier"));
        }

        @PostMapping("/{shipmentId}/label")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Fetch shipping label")
        public ResponseEntity<ApiResponse<ShipmentResponse>> fetchLabel(
                        @CurrentUserId String userId,
                        @PathVariable String shipmentId) {
                ShipmentResult result = fetchLabelInputPort.execute(new FetchLabelCommand(shipmentId, userId));
                return ResponseEntity.ok(ApiResponse.success(
                                shipmentDtoMapper.toResponse(result),
                                "Label fetched"));
        }

        @PostMapping("/{shipmentId}/cancel")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Cancel shipment (merchant-only)")
        public ResponseEntity<ApiResponse<ShipmentResponse>> cancel(
                        @CurrentUserId String userId,
                        @PathVariable String shipmentId,
                        @Valid @RequestBody CancelShipmentRequest request) {
                CancelShipmentCommand command = shipmentDtoMapper.toCommand(request, shipmentId, userId);
                return ResponseEntity.ok(ApiResponse.success(
                                shipmentDtoMapper.toResponse(cancelShipmentInputPort.execute(command)),
                                "Shipment cancelled"));
        }

        @PostMapping("/{shipmentId}/issue")
        @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
        @Operation(summary = "Resolve issue")
        public ResponseEntity<ApiResponse<ShipmentResponse>> resolveIssue(
                        @PathVariable String shipmentId,
                        @Valid @RequestBody ResolveIssueRequest request) {
                ResolveIssueCommand command = shipmentDtoMapper.toCommand(request, shipmentId);
                return ResponseEntity.ok(ApiResponse.success(
                                shipmentDtoMapper.toResponse(resolveIssueInputPort.execute(command)),
                                "Issue resolved"));
        }

        @GetMapping("/{shipmentId}")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Get shipment for the authenticated viewer (buyer or seller)")
        public ResponseEntity<ApiResponse<ShipmentResponse>> get(
                        @CurrentUserId String userId,
                        @PathVariable String shipmentId) {
                ShipmentResult result = getShipmentInputPort.execute(shipmentId, userId);
                return ResponseEntity.ok(ApiResponse.success(
                                shipmentDtoMapper.toResponse(result),
                                "Shipment fetched"));
        }

        @GetMapping("/by-order/{orderId}")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "List shipments for an order, filtered by viewer ownership")
        public ResponseEntity<ApiResponse<List<ShipmentResponse>>> listByOrder(
                        @CurrentUserId String userId,
                        @PathVariable String orderId) {
                List<ShipmentResult> results = listShipmentsByOrderInputPort.execute(orderId, userId);
                return ResponseEntity.ok(ApiResponse.success(
                                shipmentDtoMapper.toResponses(results),
                                "Shipments fetched"));
        }
}
