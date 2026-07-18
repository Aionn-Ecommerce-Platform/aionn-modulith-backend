package com.aionn.ordering.adapter.rest.controller;

import com.aionn.ordering.adapter.rest.dto.request.*;
import com.aionn.ordering.adapter.rest.dto.response.OrderResponse;
import com.aionn.ordering.adapter.rest.mapper.OrderingDtoMapper;
import com.aionn.ordering.adapter.rest.support.session.CurrentMerchantId;
import com.aionn.ordering.adapter.rest.support.session.CurrentUserId;
import com.aionn.ordering.application.dto.order.command.*;
import com.aionn.ordering.application.dto.order.result.MerchantOrderAnalyticsResult;
import com.aionn.ordering.application.dto.order.result.PlatformOrderAnalyticsResult;
import com.aionn.ordering.application.dto.order.result.TopProductResult;
import com.aionn.ordering.application.port.in.order.*;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ordering/orders")
@RequiredArgsConstructor
@Tag(name = "Ordering - Order", description = "Order placement and lifecycle")
public class OrderController {

    private final PlaceOrderInputPort placeOrderInputPort;
    private final ConfirmPreparationInputPort confirmPreparationInputPort;
    private final CancelOrderInputPort cancelOrderInputPort;
    private final RejectOrderInputPort rejectOrderInputPort;
    private final ChangeShippingInfoInputPort changeShippingInfoInputPort;
    private final ConfirmShippedInputPort confirmShippedInputPort;
    private final ConfirmDeliveredInputPort confirmDeliveredInputPort;
    private final GetOrderInputPort getOrderInputPort;
    private final ListOrdersInputPort listOrdersInputPort;
    private final GetMerchantOrderAnalyticsInputPort getMerchantOrderAnalyticsInputPort;
    private final GetPlatformOrderAnalyticsInputPort getPlatformOrderAnalyticsInputPort;
    private final GetTopProductsInputPort getTopProductsInputPort;
    private final OrderingDtoMapper dtoMapper;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Place order")
    public ResponseEntity<ApiResponse<OrderResponse>> place(
            @CurrentUserId String userId,
            @Valid @RequestBody PlaceOrderRequest request) {
        OrderResponse response = dtoMapper.toResponse(placeOrderInputPort.execute(new PlaceOrderCommand(
                userId,
                request.addressId(),
                request.paymentMethodId(),
                request.currency(),
                request.shippingFee(),
                request.shippingAddress(),
                request.selectedSkuIds(),
                request.gateway())));
        return ApiResponse.createdResponse("Order placed", response);
    }

    @PostMapping("/{orderId}/confirm-preparation")
    @PreAuthorize("hasAnyAuthority('ROLE_MERCHANT','ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Merchant confirms preparation")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmPreparation(
            @CurrentMerchantId String ownerId,
            @PathVariable String orderId,
            @Valid @RequestBody(required = false) ConfirmPreparationRequest request) {
        OrderResponse response = dtoMapper.toResponse(confirmPreparationInputPort.execute(
                new ConfirmPreparationCommand(orderId, ownerId)));
        return ResponseEntity.ok(ApiResponse.success(response, "Preparation confirmed"));
    }

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "User cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(
            @CurrentUserId String userId,
            @PathVariable String orderId,
            @Valid @RequestBody CancelOrderRequest request) {
        OrderResponse response = dtoMapper.toResponse(cancelOrderInputPort.execute(new CancelOrderCommand(
                orderId, userId, request.reason())));
        return ResponseEntity.ok(ApiResponse.success(response, "Order cancelled"));
    }

    @PostMapping("/{orderId}/reject")
    @PreAuthorize("hasAnyAuthority('ROLE_MERCHANT','ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Merchant reject")
    public ResponseEntity<ApiResponse<OrderResponse>> reject(
            @CurrentMerchantId String ownerId,
            @PathVariable String orderId,
            @Valid @RequestBody RejectOrderRequest request) {
        OrderResponse response = dtoMapper.toResponse(rejectOrderInputPort.execute(new RejectOrderCommand(
                orderId, ownerId, request.reason())));
        return ResponseEntity.ok(ApiResponse.success(response, "Order rejected"));
    }

    @PutMapping("/{orderId}/shipping-info")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Change shipping info")
    public ResponseEntity<ApiResponse<OrderResponse>> changeShippingInfo(
            @CurrentUserId String userId,
            @PathVariable String orderId,
            @Valid @RequestBody ChangeShippingInfoRequest request) {
        OrderResponse response = dtoMapper.toResponse(changeShippingInfoInputPort.execute(new ChangeShippingInfoCommand(
                orderId, userId, request.newAddress(), request.newShippingFee())));
        return ResponseEntity.ok(ApiResponse.success(response, "Shipping info changed"));
    }

    @PostMapping("/{orderId}/ship")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Mark shipped")
    public ResponseEntity<ApiResponse<OrderResponse>> ship(
            @PathVariable String orderId,
            @Valid @RequestBody ConfirmShippedRequest request) {
        OrderResponse response = dtoMapper.toResponse(confirmShippedInputPort.execute(
                new ConfirmShippedCommand(orderId, request.shipmentId())));
        return ResponseEntity.ok(ApiResponse.success(response, "Order shipped"));
    }

    @PostMapping("/{orderId}/complete")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Complete order")
    public ResponseEntity<ApiResponse<OrderResponse>> complete(@PathVariable String orderId) {
        OrderResponse response = dtoMapper.toResponse(confirmDeliveredInputPort.execute(new ConfirmDeliveredCommand(orderId)));
        return ResponseEntity.ok(ApiResponse.success(response, "Order completed"));
    }

    @GetMapping("/merchant")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "List merchant orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> listForMerchant(
            @CurrentMerchantId String ownerId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        List<OrderResponse> responses = listOrdersInputPort.execute(
                ownerId, "MERCHANT", status, safeLimit).stream()
                .map(dtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses, "Orders fetched"));
    }

    @GetMapping("/merchant/analytics")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "Get merchant order analytics")
    public ResponseEntity<ApiResponse<MerchantOrderAnalyticsResult>> merchantAnalytics(
            @CurrentMerchantId String ownerId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        MerchantOrderAnalyticsResult result = getMerchantOrderAnalyticsInputPort.execute(ownerId, from, to);
        return ResponseEntity.ok(ApiResponse.success(result, "Merchant analytics fetched"));
    }

    @GetMapping("/merchant/top-products")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "Top products by revenue for merchant")
    public ResponseEntity<ApiResponse<List<TopProductResult>>> merchantTopProducts(
            @CurrentMerchantId String ownerId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "5") int limit) {
        List<TopProductResult> result = getTopProductsInputPort.execute(ownerId, from, to, limit);
        return ResponseEntity.ok(ApiResponse.success(result, "Top products fetched"));
    }

    @GetMapping("/admin/analytics")
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Platform-wide order analytics (sysadmin)")
    public ResponseEntity<ApiResponse<PlatformOrderAnalyticsResult>> platformAnalytics(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        PlatformOrderAnalyticsResult result = getPlatformOrderAnalyticsInputPort.execute(from, to);
        return ResponseEntity.ok(ApiResponse.success(result, "Platform analytics fetched"));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get order")
    public ResponseEntity<ApiResponse<OrderResponse>> get(
            @CurrentUserId String userId,
            @PathVariable String orderId) {
        OrderResponse response = dtoMapper.toResponse(getOrderInputPort.execute(orderId, userId));
        return ResponseEntity.ok(ApiResponse.success(response, "Order fetched"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List my orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> listMine(
            @CurrentUserId String userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        List<OrderResponse> responses = listOrdersInputPort.execute(
                userId, "USER", status, safeLimit).stream()
                .map(dtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses, "Orders fetched"));
    }
}
