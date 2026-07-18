package com.aionn.ordering.adapter.rest.controller;

import com.aionn.ordering.adapter.rest.dto.request.ApproveReturnRequest;
import com.aionn.ordering.adapter.rest.dto.request.ConfirmItemReceivedRequest;
import com.aionn.ordering.adapter.rest.dto.request.RejectReturnRequest;
import com.aionn.ordering.adapter.rest.dto.request.RequestReturnRequest;
import com.aionn.ordering.adapter.rest.dto.response.OrderReturnResponse;
import com.aionn.ordering.adapter.rest.mapper.OrderingDtoMapper;
import com.aionn.ordering.adapter.rest.support.session.CurrentMerchantId;
import com.aionn.ordering.adapter.rest.support.session.CurrentUserId;
import com.aionn.ordering.application.dto.returns.command.ApproveReturnCommand;
import com.aionn.ordering.application.dto.returns.command.ConfirmItemReceivedCommand;
import com.aionn.ordering.application.dto.returns.command.RejectReturnCommand;
import com.aionn.ordering.application.dto.returns.command.RequestReturnCommand;
import com.aionn.ordering.application.port.in.returns.*;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ordering/returns")
@RequiredArgsConstructor
@Tag(name = "Ordering - Return", description = "Order return / refund flow")
public class OrderReturnController {

    private final RequestReturnInputPort requestReturnInputPort;
    private final ApproveReturnInputPort approveReturnInputPort;
    private final RejectReturnInputPort rejectReturnInputPort;
    private final ConfirmItemReceivedInputPort confirmItemReceivedInputPort;
    private final GetReturnInputPort getReturnInputPort;
    private final ListReturnsInputPort listReturnsInputPort;
    private final OrderingDtoMapper dtoMapper;

    @PostMapping("/orders/{orderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Request return")
    public ResponseEntity<ApiResponse<OrderReturnResponse>> request(
            @CurrentUserId String userId,
            @PathVariable String orderId,
            @Valid @RequestBody RequestReturnRequest request) {
        OrderReturnResponse response = dtoMapper.toResponse(requestReturnInputPort.execute(new RequestReturnCommand(
                orderId, userId, request.reason(), request.evidenceUrl())));
        return ApiResponse.createdResponse("Return requested", response);
    }

    @PostMapping("/{returnId}/approve")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Merchant approve return")
    public ResponseEntity<ApiResponse<OrderReturnResponse>> approve(
            @CurrentMerchantId String ownerId,
            @PathVariable String returnId,
            @Valid @RequestBody ApproveReturnRequest request) {
        OrderReturnResponse response = dtoMapper.toResponse(approveReturnInputPort.execute(new ApproveReturnCommand(
                returnId, ownerId,
                request.refundAmount(), request.currency(), request.returnWarehouseId())));
        return ResponseEntity.ok(ApiResponse.success(response, "Return approved"));
    }

    @PostMapping("/{returnId}/reject")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Merchant reject return")
    public ResponseEntity<ApiResponse<OrderReturnResponse>> reject(
            @CurrentMerchantId String ownerId,
            @PathVariable String returnId,
            @Valid @RequestBody RejectReturnRequest request) {
        OrderReturnResponse response = dtoMapper.toResponse(rejectReturnInputPort.execute(new RejectReturnCommand(
                returnId, ownerId, request.reason())));
        return ResponseEntity.ok(ApiResponse.success(response, "Return rejected"));
    }

    @PostMapping("/{returnId}/item-received")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Merchant confirms item received")
    public ResponseEntity<ApiResponse<OrderReturnResponse>> confirmReceived(
            @CurrentMerchantId String ownerId,
            @PathVariable String returnId,
            @Valid @RequestBody ConfirmItemReceivedRequest request) {
        OrderReturnResponse response = dtoMapper.toResponse(confirmItemReceivedInputPort.execute(new ConfirmItemReceivedCommand(
                returnId, ownerId, request.itemCondition())));
        return ResponseEntity.ok(ApiResponse.success(response, "Return item received"));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List return requests by the authenticated user")
    public ResponseEntity<ApiResponse<List<OrderReturnResponse>>> listMine(
            @CurrentUserId String userId,
            @RequestParam(defaultValue = "50") int limit) {
        List<OrderReturnResponse> responses = listReturnsInputPort.execute(userId, "USER", limit).stream()
                .map(dtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses, "Returns fetched"));
    }

    @GetMapping("/merchant")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List return requests for the merchant")
    public ResponseEntity<ApiResponse<List<OrderReturnResponse>>> listMerchant(
            @CurrentMerchantId String ownerId,
            @RequestParam(defaultValue = "50") int limit) {
        List<OrderReturnResponse> responses = listReturnsInputPort.execute(ownerId, "MERCHANT", limit).stream()
                .map(dtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses, "Returns fetched"));
    }

    @GetMapping("/{returnId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get return")
    public ResponseEntity<ApiResponse<OrderReturnResponse>> get(
            @CurrentUserId String userId,
            @PathVariable String returnId) {
        OrderReturnResponse response = dtoMapper.toResponse(getReturnInputPort.execute(returnId, userId));
        return ResponseEntity.ok(ApiResponse.success(response, "Return fetched"));
    }
}
